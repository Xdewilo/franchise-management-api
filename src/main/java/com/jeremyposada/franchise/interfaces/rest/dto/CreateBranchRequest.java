package com.jeremyposada.franchise.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo para abrir una sucursal.
 *
 * @param name nombre de la sucursal
 */
@Schema(description = "Datos para abrir una sucursal")
public record CreateBranchRequest(

        @Schema(description = "Nombre de la sucursal", example = "Sucursal Norte")
        @NotBlank(message = "El nombre de la sucursal es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name
) {}
