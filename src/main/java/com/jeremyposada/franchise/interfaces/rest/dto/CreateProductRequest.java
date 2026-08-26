package com.jeremyposada.franchise.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo para incorporar un producto al catálogo de una sucursal.
 *
 * @param name  nombre del producto
 * @param stock existencias iniciales
 */
@Schema(description = "Datos para incorporar un producto a una sucursal")
public record CreateProductRequest(

        @Schema(description = "Nombre del producto", example = "Arepa de huevo")
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        @Schema(description = "Existencias iniciales", example = "25")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Long stock
) {}
