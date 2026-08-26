variable "neon_api_key" {
  description = "API key de Neon. Se obtiene en https://console.neon.tech/app/settings/api-keys"
  type        = string
  sensitive   = true
}

variable "project_name" {
  description = "Nombre del proyecto en Neon"
  type        = string
  default     = "franchise-management-api"
}

variable "region" {
  description = "Región del proyecto. Debe coincidir con la del servicio en Render: colocalizar cómputo y base evita que cada consulta cruce medio continente"
  type        = string
  default     = "aws-us-east-2"
}

variable "postgres_version" {
  description = "Versión mayor de PostgreSQL"
  type        = number
  default     = 16
}

variable "database_name" {
  description = "Nombre de la base de datos de la aplicación"
  type        = string
  default     = "franchises"
}

variable "database_owner" {
  description = "Rol propietario de la base de datos"
  type        = string
  default     = "franchise"
}
