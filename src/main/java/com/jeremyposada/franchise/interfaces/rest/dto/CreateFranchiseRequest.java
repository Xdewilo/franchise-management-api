package com.jeremyposada.franchise.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo para registrar una franquicia.
 *
 * @param name nombre comercial
 */
@Schema(description = "Datos para registrar una franquicia")
public record CreateFranchiseRequest(

        @Schema(description = "Nombre comercial de la franquicia", example = "Vive Fresh")
        @NotBlank(message = "El nombre de la franquicia es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name
) {}
