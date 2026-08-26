# =============================================================================
# Proveedores y versiones
#
# Las versiones se fijan con `~>` para que `terraform init` no introduzca
# cambios mayores sin decisión explícita: la infraestructura debe ser
# reproducible, no depender de qué día se ejecute.
# =============================================================================

terraform {
  required_version = ">= 1.6"

  required_providers {
    neon = {
      source  = "kislerdm/neon"
      version = "~> 0.9"
    }
  }
}

provider "neon" {
  # La API key se toma de la variable de entorno NEON_API_KEY o de
  # terraform.tfvars, que está excluido del control de versiones.
  api_key = var.neon_api_key
}
