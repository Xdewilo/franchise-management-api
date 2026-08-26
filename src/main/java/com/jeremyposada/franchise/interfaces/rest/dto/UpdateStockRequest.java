package com.jeremyposada.franchise.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cuerpo para fijar las existencias de un producto.
 *
 * <p>El valor es absoluto, no un incremento: repetir la misma petición deja el
 * mismo resultado.
 *
 * @param stock existencias nuevas
 */
@Schema(description = "Existencias nuevas del producto")
public record UpdateStockRequest(

        @Schema(description = "Cantidad absoluta de existencias", example = "40")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Long stock
) {}
