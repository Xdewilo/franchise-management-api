package com.jeremyposada.franchise.interfaces.rest.dto;

import com.jeremyposada.franchise.application.PagedResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltorio de un listado paginado.
 *
 * @param items      elementos de la página
 * @param page       número de página, base cero
 * @param size       tamaño de página aplicado
 * @param totalItems total de elementos existentes
 * @param totalPages total de páginas
 * @param <T>        tipo de los elementos
 */
@Schema(description = "Página de resultados")
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    /**
     * @param result  resultado paginado del caso de uso
     * @param mapper  conversión de cada elemento a su forma pública
     * @param <D>     tipo de dominio
     * @param <T>     tipo expuesto
     * @return la página lista para serializar
     */
    public static <D, T> PageResponse<T> from(PagedResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(
                result.items().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages());
    }
}
