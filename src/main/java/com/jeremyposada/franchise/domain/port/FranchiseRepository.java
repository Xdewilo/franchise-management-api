package com.jeremyposada.franchise.domain.port;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.TopStockProduct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Puerto de salida hacia el almacenamiento de franquicias.
 *
 * <p>Lo define el dominio y lo implementa la infraestructura: la dirección de
 * la dependencia apunta hacia adentro, que es lo que permite testear los casos
 * de uso con un doble en memoria, sin base de datos ni contexto de Spring.
 *
 * <p><b>Sobre {@code Mono}/{@code Flux} en el dominio.</b> Un purista exigiría
 * tipos del JDK. Se admite Reactor aquí a conciencia: es el contrato de
 * asincronía de toda la aplicación, y sustituirlo por {@code CompletableFuture}
 * obligaría a convertir en cada frontera sin ganar independencia real.
 */
public interface FranchiseRepository {

    /**
     * Persiste el agregado completo, insertándolo o actualizándolo según
     * corresponda.
     *
     * @param franchise agregado a guardar
     * @return el agregado guardado, con la versión ya incrementada
     */
    Mono<Franchise> save(Franchise franchise);

    /**
     * Recupera un agregado completo.
     *
     * @param franchiseId identidad de la franquicia
     * @return el agregado, o vacío si no existe
     */
    Mono<Franchise> findById(UUID franchiseId);

    /**
     * Indica si el nombre ya está tomado por otra franquicia.
     *
     * @param name      nombre a verificar
     * @param excluding franquicia que se excluye de la búsqueda (la que se
     *                  está renombrando), o {@code null} para no excluir ninguna
     * @return {@code true} si el nombre ya pertenece a otra franquicia
     */
    Mono<Boolean> existsByName(Name name, UUID excluding);

    /**
     * Lista franquicias de forma paginada, ordenadas por nombre.
     *
     * @param page número de página, base cero
     * @param size tamaño de página
     * @return las franquicias de esa página
     */
    Flux<Franchise> findAll(int page, int size);

    /** @return total de franquicias registradas */
    Mono<Long> count();

    /**
     * Resuelve el criterio 7: el producto con más stock de cada sucursal de la
     * franquicia indicada.
     *
     * <p>Se declara como operación del puerto —y no como recorrido en memoria
     * sobre el agregado— para que el adaptador pueda resolverlo dentro del
     * motor de base de datos, que es lo que sostiene el crecimiento del
     * catálogo.
     *
     * @param franchiseId identidad de la franquicia
     * @return un producto por sucursal; las sucursales sin productos no aparecen
     */
    Flux<TopStockProduct> findTopStockProductPerBranch(UUID franchiseId);
}
