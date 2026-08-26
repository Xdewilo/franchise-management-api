package com.jeremyposada.franchise.interfaces.rest.dto;

import com.jeremyposada.franchise.domain.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Producto tal como se expone en el API.
 *
 * @param id    identidad del producto
 * @param name  nombre del producto
 * @param stock existencias
 */
@Schema(description = "Producto ofertado en una sucursal")
public record ProductResponse(UUID id, String name, long stock) {

    /**
     * @param product producto del dominio
     * @return su representación pública
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id(), product.name().value(), product.stock().value());
    }
}
