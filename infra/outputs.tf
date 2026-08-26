# =============================================================================
# Salidas
#
# Todo lo que expone credenciales se marca como `sensitive`, de modo que
# Terraform no lo imprima en los logs de un pipeline. Para leerlas:
#   terraform output -raw r2dbc_url
# =============================================================================

output "host" {
  description = "Host del endpoint de PostgreSQL"
  value       = neon_project.franchises.database_host
}

output "database_name" {
  description = "Nombre de la base de datos"
  value       = neon_database.franchises.name
}

output "username" {
  description = "Usuario de la aplicación"
  value       = neon_role.application.name
}

output "password" {
  description = "Contraseña del usuario de la aplicación"
  value       = neon_role.application.password
  sensitive   = true
}

output "r2dbc_url" {
  description = "URL R2DBC lista para la variable de entorno de la aplicación"
  value       = "r2dbc:postgresql://${neon_project.franchises.database_host}/${neon_database.franchises.name}"
  sensitive   = true
}

output "connection_uri" {
  description = "Cadena de conexión completa (incluye credenciales)"
  value       = neon_project.franchises.connection_uri
  sensitive   = true
}
