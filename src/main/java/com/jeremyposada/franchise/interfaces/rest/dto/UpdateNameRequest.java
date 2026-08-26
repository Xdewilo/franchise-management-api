package com.jeremyposada.franchise.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo compartido por las tres operaciones de renombrado (franquicia,
 * sucursal y producto): las tres reciben exactamente el mismo dato.
 *
 * @param name nombre nuevo
 */
@Schema(description = "Nombre nuevo a asignar")
public record UpdateNameRequest(

        @Schema(description = "Nombre nuevo", example = "Vive Fresh Premium")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name
) {}
