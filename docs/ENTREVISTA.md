# Guion de defensa técnica

> Documento interno de preparación. No forma parte de la entrega.

Cada sección es una pregunta probable, con la respuesta corta que conviene dar primero y el detalle para cuando pregunten más.

---

## 1. «¿Por qué guardaste todo en una columna JSONB en vez de tres tablas?»

**Respuesta corta:** porque la frontera del agregado coincide con la frontera transaccional.

**El detalle:** `Franquicia` es el aggregate root. Sucursales y productos no tienen ciclo de vida propio —no se consultan ni se modifican fuera de su franquicia— y todas las reglas de unicidad se evalúan sobre el árbol completo. Al guardarlo en una fila, agregar un producto, borrarlo o ajustar el stock es una sola escritura atómica: no hay transacciones sobre varias tablas ni posibilidad de estado a medias. Y leer la franquicia es una fila, sin JOINs ni N+1.

**El trade-off, dicho antes de que lo señalen:** si una franquicia acumulara decenas de miles de productos, el documento crecería y cada escritura reescribiría la fila entera. Para el volumen que describe el enunciado está muy lejos de ser un problema. Si lo fuera, sacaría los productos a su propia tabla — y el dominio no cambiaría, sólo el adaptador, porque el puerto ya aísla a la aplicación del esquema.

**Si insisten con «¿y por qué no Mongo, entonces?»:** el modelo documental encajaba igual de bien, pero PostgreSQL con JSONB da lo mismo más integridad referencial, índices únicos reales (el de `lower(name)`), transacciones y SQL para consultas analíticas como la del criterio 7. No hacía falta renunciar a nada de eso.

---

## 2. «¿Por qué no usaste JPA?»

Porque JPA es bloqueante por diseño. Bajo WebFlux, cada consulta retendría un hilo del *event loop* y anularía la ventaja del modelo reactivo — sería un stack reactivo sólo de nombre.

R2DBC es no bloqueante de extremo a extremo. El coste es que no ofrece relaciones, *lazy loading* ni *dirty checking*; aquí ese coste es irrelevante, porque el agregado se persiste como un único documento y no hay relaciones que mapear.

---

## 3. «¿Qué es R2DBC exactamente?»

*Reactive Relational Database Connectivity*: la especificación reactiva equivalente a JDBC. El driver no bloquea el hilo esperando la respuesta de la base de datos; devuelve `Mono`/`Flux` y libera el hilo mientras tanto.

No es JPA: Spring Data R2DBC es deliberadamente un mapeador simple, sin relaciones ni contexto de persistencia.

---

## 4. «¿Por qué SQL a mano en vez de un repositorio de Spring Data?»

Dos razones concretas:

1. **La identidad se genera en la aplicación.** Con un `ReactiveCrudRepository`, una entidad que ya trae el id se interpreta como existente y Spring Data intenta un `UPDATE` en vez de un `INSERT`. Se puede rodear implementando `Persistable`, pero eso es añadir código para pelear con una heurística.
2. **El bloqueo optimista queda explícito.** El `UPDATE ... WHERE id = ? AND version = ?` se lee tal cual se ejecuta, en lugar de depender de lo que el framework decida generar.

Y el criterio 7 es una consulta con `LATERAL` y `DISTINCT ON` que ningún repositorio derivado puede expresar de todas formas.

---

## 5. «Explícame la consulta del criterio 7»

```sql
SELECT DISTINCT ON (branch.value ->> 'id') ...
  FROM franchises f
  CROSS JOIN LATERAL jsonb_array_elements(f.branches)               AS branch(value)
  CROSS JOIN LATERAL jsonb_array_elements(branch.value->'products') AS product(value)
 WHERE f.id = :franchiseId
 ORDER BY branch.value ->> 'id',
          (product.value ->> 'stock')::bigint DESC,
          product.value ->> 'name' ASC;
```

- El primer `LATERAL` despliega el array de sucursales en una fila por sucursal.
- El segundo, para cada sucursal, despliega sus productos: el resultado intermedio es una fila por producto.
- `DISTINCT ON (sucursal)` se queda con **la primera fila de cada grupo según el `ORDER BY`** — y como el orden es stock descendente, esa primera fila es el producto con más stock.
- El tercer criterio de orden, el nombre, hace la respuesta determinista cuando dos productos empatan en existencias. Sin él, la base podría devolver cualquiera de los dos.

**Por qué en SQL y no en Java:** con datos de prueba da igual. Con un catálogo grande, hacerlo en Java obliga a traer el árbol completo a la JVM para devolver una fila por sucursal.

**`DISTINCT ON` es específico de PostgreSQL.** El equivalente portable sería una *window function* — `ROW_NUMBER() OVER (PARTITION BY sucursal ORDER BY stock DESC)` filtrando por `= 1`.

---

## 6. «¿Por qué no usaste el patrón Mediator, si lo conoces?»

Ésta conviene responderla con seguridad, porque es la que mide criterio y no conocimiento.

Un Mediator paga cuando hay muchos handlers, **varios puntos de entrada** —REST y consumidores de mensajería, por ejemplo— y un pipeline transversal (validación, trazas, transacciones) que conviene centralizar. En mi trabajo actual lo implementé por eso: cinco subdominios, entrada por REST y por Kafka.

Aquí hay un agregado y diez operaciones, con un único punto de entrada. Añadirlo cambiaría una llamada directa y verificada en compilación por una resolución por reflexión en tiempo de ejecución, sin reducir acoplamiento real, y metería una clase más que leer antes de llegar a la lógica de negocio.

**El principio:** añadir complejidad sólo cuando el problema la paga. Y cuando se decide no añadirla, dejarlo escrito — está en la sección de trade-offs del README.

---

## 7. «¿Cómo manejas la concurrencia?»

Con bloqueo optimista, y es una consecuencia directa de guardar el agregado completo.

**El problema:** dos peticiones simultáneas sobre la misma franquicia leen, modifican y escriben. Sin protección, la segunda pisa a la primera: un producto agregado desaparece sin ningún error visible.

**La solución:** la columna `version` viaja en el `WHERE` del `UPDATE`. Si otra petición ya escribió, ninguna fila coincide, la operación no toca nada y se reporta como conflicto.

**Y algo más:** ante ese conflicto, `FranchiseMutator` reintenta el ciclo completo releyendo el estado ya actualizado, con espera exponencial. Sólo si el choque persiste tras varios intentos se devuelve `409`. En la práctica, dos peticiones que agregan productos distintos acaban aplicándose las dos.

Está probado en ambas direcciones: contra PostgreSQL real se verifica que la escritura obsoleta se rechaza, y en la suite de aplicación que el reintento ocurre y que se agota correctamente.

**Si preguntan por pesimista:** un `SELECT FOR UPDATE` retendría la fila y serializaría todo el tráfico de esa franquicia. Con conflictos poco frecuentes, el optimista es más barato: no cuesta nada en el caso normal y sólo trabaja cuando de verdad hay choque.

**Dato concreto que puedes citar:** con 12 peticiones lanzadas *en paralelo* agregando sucursales distintas a la misma franquicia, las 12 respondieron `201` y las 12 quedaron persistidas, con el agregado en versión 12. Sin bloqueo optimista, la mayoría se habría perdido en silencio.

---

## 8. «¿Por qué el dominio es inmutable?»

Tres motivos, en orden de importancia:

1. **Imposibilita el estado inválido.** No hay setters; cada operación pasa por un método que verifica la invariante antes de construir la instancia nueva.
2. **Es seguro entre hilos.** En un stack reactivo la ejecución salta de hilo constantemente; con objetos inmutables eso deja de ser una preocupación.
3. **Encaja con el enfoque funcional** que pedía el enunciado: las operaciones son transformaciones de un valor a otro, no mutaciones.

El coste —crear objetos nuevos en cada operación— es despreciable: son árboles de decenas de elementos y el recolector de la JVM está optimizado precisamente para objetos de vida corta.

---

## 9. «¿Por qué validas dos veces, en el DTO y en el dominio?»

Porque hacen cosas distintas:

- La del **DTO** existe para dar un `400` con la **lista completa** de campos rechazados de una vez. Es una cuestión de experiencia de uso del API.
- La del **dominio** es la garantía real: se cumple venga la petición de donde venga —REST hoy, mensajería mañana, un test— y hace imposible construir un objeto inválido.

Si sólo existiera la primera, la invariante dependería del adaptador de turno. Si sólo existiera la segunda, el cliente descubriría los errores de uno en uno.

---

## 10. «¿Cómo escalarías esto?»

Por orden de lo que aparecería primero:

| Presión | Respuesta |
|---|---|
| Catálogos enormes | Productos a su propia tabla. Cambia el adaptador, no el dominio |
| Muchas más lecturas que escrituras | Proyección de lectura separada (CQRS con modelos distintos), alimentada por eventos de dominio |
| Integración con otros servicios | Eventos de dominio hacia un *outbox* transaccional y de ahí a mensajería |
| Contención en una misma franquicia | Stock como operación incremental (`SET stock = stock + n`), que no necesita leer antes de escribir |
| Multi-tenencia | JWT en el borde y aislamiento por esquema o Row-Level Security |

Lo que hace viable cualquiera de esos cambios es que el dominio no conoce a la infraestructura.

---

## 10.b «¿Qué problema te dio el despliegue?»

Una pregunta muy habitual, y conviene tener una respuesta concreta en vez de "ninguno".

**Tres cosas aparecieron sólo al aplicar la infraestructura de verdad**, ninguna detectable revisando el código:

1. **Neon exige `org_id`** al crear proyectos por API. Lo expuse como variable de Terraform en lugar de fijarlo, para que la definición sirva a cualquier organización.
2. **La retención de historial topa en 6 horas** en el plan gratuito; pedía 24 y la API rechazaba la creación.
3. **Neon rechaza conexiones sin cifrar**, y los dos drivers nombran distinto el parámetro: `sslMode` en R2DBC, `sslmode` en JDBC. Como derivo la URL de Flyway de la de R2DBC, la traducción hacía falta o Flyway fallaba al migrar en el arranque.

Y una cuarta, de comportamiento en ejecución: **contra un PostgreSQL serverless el pool entregaba conexiones ya cerradas**. Neon suspende el cómputo por inactividad y cierra las conexiones ociosas; sin validar antes de prestar, la petición fallaba sin que nada en el código estuviera mal. Se resolvió con `validation-query` y acortando la vida ociosa de 30 a 5 minutos.

Es el argumento de por qué la infraestructura como código hay que *aplicarla*, no sólo escribirla: `terraform validate` pasaba en verde con los tres primeros errores dentro.

---

## 11. «¿Qué mejorarías si tuvieras más tiempo?»

Conviene tener la respuesta preparada, y que sea honesta:

- **Tests de carga** para fijar la línea base de latencia y ver cuándo empieza a doler el agregado grande.
- **Trazabilidad distribuida** (OpenTelemetry) y métricas de negocio en Prometheus — en mi trabajo actual la plataforma tiene Grafana, Loki y Jaeger, y aquí lo dejé fuera por alcance.
- **Backend remoto para el estado de Terraform**: hoy vive en local, que no sirve para un equipo.
- **Autenticación**: el enunciado no la pedía y meterla habría sido salirse del alcance, pero en producción es lo primero que añadiría.
- **Índice GIN aprovechado de verdad**: hoy está creado pero ninguna consulta lo usa todavía; entraría con la búsqueda de productos por nombre.

---

## 12. Datos por si preguntan

| | |
|---|---|
| Pruebas | 74 (dominio, aplicación e integración) |
| Endpoints | 11 (7 de criterios + 3 extra + consulta de apoyo) |
| Criterios cumplidos | 8 de 8 |
| Puntos extra cumplidos | 7 de 7 |
| Java | 21 (toolchain) |
| Spring Boot | 3.5.16 |
| PostgreSQL | 16 |

**Por qué Spring Boot 3.5 y no 4.x:** 3.5 es la línea estable con soporte extendido y el ecosistema alineado (springdoc, Testcontainers). En una prueba con fecha de entrega, elegir la versión que garantiza compatibilidad es parte del criterio.
