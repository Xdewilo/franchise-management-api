# =============================================================================
# Persistencia de la aplicación: PostgreSQL gestionado en Neon.
#
# El objetivo es que la base de datos sea reproducible: cualquiera con una API
# key puede levantar una réplica idéntica del entorno con `terraform apply`,
# sin pasos manuales en una consola web que nadie documenta.
#
# El esquema NO se crea aquí. De eso se encarga Flyway durante el arranque de
# la aplicación, que es lo que mantiene el versionado del esquema junto al
# código que lo usa. Terraform aprovisiona el recipiente; Flyway lo llena.
# =============================================================================

resource "neon_project" "franchises" {
  org_id     = var.neon_org_id
  name       = var.project_name
  region_id  = var.region
  pg_version = var.postgres_version

  # El plan gratuito suspende el cómputo tras un periodo de inactividad y lo
  # reanuda con la siguiente conexión. Es la razón de que la primera petición
  # tras un rato sin uso tarde más de lo normal.
  #
  # La ventana de recuperación puntual (point-in-time restore) topa en 6 horas
  # en el plan gratuito; pedir más hace que la API rechace la creación.
  history_retention_seconds = var.history_retention_seconds
}

resource "neon_role" "application" {
  project_id = neon_project.franchises.id
  branch_id  = neon_project.franchises.default_branch_id
  name       = var.database_owner
}

resource "neon_database" "franchises" {
  project_id = neon_project.franchises.id
  branch_id  = neon_project.franchises.default_branch_id
  name       = var.database_name
  owner_name = neon_role.application.name
}
