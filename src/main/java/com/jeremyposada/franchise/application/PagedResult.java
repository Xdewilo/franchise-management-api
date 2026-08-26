package com.jeremyposada.franchise.application;

import java.util.List;

/**
 * Fragmento de un listado junto con los datos necesarios para navegarlo.
 *
 * @param items      elementos de la página actual
 * @param page       número de página, base cero
 * @param size       tamaño de página solicitado
 * @param totalItems total de elementos existentes
 * @param <T>        tipo de los elementos
 */
public record PagedResult<T>(List<T> items, int page, int size, long totalItems) {

    /** @return número total de páginas para el tamaño solicitado */
    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalItems / size);
    }
}
