# Infraestructura

Aprovisionamiento de la base de datos PostgreSQL en [Neon](https://neon.tech) mediante Terraform.

## Qué se crea

| Recurso | Descripción |
|---|---|
| `neon_project` | Proyecto con su rama principal y su endpoint de cómputo |
| `neon_role` | Rol de la aplicación, con contraseña generada por Neon |
| `neon_database` | Base de datos `franchises`, propiedad de ese rol |

**El esquema no se crea aquí.** De eso se encarga Flyway durante el arranque de la aplicación, que es lo que mantiene el versionado del esquema junto al código que lo usa. Terraform aprovisiona el recipiente; Flyway lo llena.

## Uso

```bash
# 1. Credenciales — obtén la API key en console.neon.tech → Settings → API Keys
cp terraform.tfvars.example terraform.tfvars
$EDITOR terraform.tfvars

# 2. Descargar el proveedor
terraform init

# 3. Revisar qué se va a crear
terraform plan

# 4. Aplicar
terraform apply

# 5. Leer la conexión para configurar la aplicación
terraform output -raw r2dbc_url
terraform output -raw password
```

Para destruirlo todo: `terraform destroy`.

## Seguridad

- `terraform.tfvars` y los ficheros de estado están en `.gitignore`: **ninguna credencial llega al repositorio**.
- Las salidas que exponen credenciales están marcadas como `sensitive`, de modo que Terraform no las imprime en los logs de un pipeline.
- El estado se guarda en local. Para un equipo, el paso siguiente sería un backend remoto (S3 con bloqueo en DynamoDB, o Terraform Cloud) para que el estado no viva en un solo portátil.

## Coste

El plan gratuito de Neon cubre este proyecto por completo. Suspende el cómputo tras un periodo de inactividad y lo reanuda al conectarse, así que **la primera petición tras un rato sin uso tarda más de lo normal**.
