package com.jeremyposada.franchise.interfaces.rest.dto;

import com.jeremyposada.franchise.domain.model.TopStockProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Respuesta del criterio 7: producto de mayor stock junto con la sucursal a la
 * que pertenece.
 *
 * @param branchId    identidad de la sucursal
 * @param branchName  nombre de la sucursal
 * @param productId   identidad del producto
 * @param productName nombre del producto
 * @param stock       existencias
 */
@Schema(description = "Producto con mayor stock de una sucursal")
public record TopStockProductResponse(
        UUID branchId,
        String branchName,
        UUID productId,
        String productName,
        long stock
) {

    /**
     * @param projection proyección de dominio
     * @return su representación pública
     */
    public static TopStockProductResponse from(TopStockProduct projection) {
        return new TopStockProductResponse(
                projection.branchId(),
                projection.branchName(),
                projection.productId(),
                projection.productName(),
                projection.stock());
    }
}
