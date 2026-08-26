package com.jeremyposada.franchise.domain.model;

import java.util.UUID;

/**
 * Producto con más stock de una sucursal concreta.
 *
 * <p>Es un modelo de lectura, no una entidad: responde al criterio 7 del
 * enunciado —«el producto que más stock tiene por sucursal para una franquicia
 * puntual, indicando a qué sucursal pertenece»— y por eso lleva incrustados
 * los datos de la sucursal.
 *
 * @param branchId    identidad de la sucursal
 * @param branchName  nombre de la sucursal a la que pertenece el producto
 * @param productId   identidad del producto
 * @param productName nombre del producto
 * @param stock       existencias, las mayores de esa sucursal
 */
public record TopStockProduct(
        UUID branchId,
        String branchName,
        UUID productId,
        String productName,
        long stock
) {}
