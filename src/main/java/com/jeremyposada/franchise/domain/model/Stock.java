package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.ValidationException;

/**
 * Cantidad disponible de un producto en una sucursal.
 *
 * <p>La invariante «el stock nunca es negativo» vive aquí, de modo que ninguna
 * ruta del código —ni la creación del producto ni el ajuste posterior— pueda
 * saltársela.
 *
 * @param value unidades disponibles, siempre {@code >= 0}
 */
public record Stock(long value) {

    /** Stock inicial de un producto que aún no tiene existencias. */
    public static final Stock ZERO = new Stock(0L);

    public Stock {
        if (value < 0) {
            throw ValidationException.negativeStock(value);
        }
    }

    /**
     * Construye un stock a partir de un valor que puede venir nulo desde el
     * borde del sistema, tratando la ausencia como cero.
     *
     * @param value unidades recibidas, posiblemente nulas
     * @return el stock correspondiente
     */
    public static Stock of(Long value) {
        return value == null ? ZERO : new Stock(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
