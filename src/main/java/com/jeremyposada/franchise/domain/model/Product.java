package com.jeremyposada.franchise.domain.model;

import java.util.UUID;

/**
 * Producto ofertado en una sucursal.
 *
 * <p>Es una entidad interna del agregado {@link Franchise}: tiene identidad
 * propia ({@code id}) pero no se accede ni se persiste por separado. Es
 * inmutable, así que cada cambio produce una instancia nueva.
 *
 * @param id    identidad estable del producto dentro del agregado
 * @param name  nombre comercial, único dentro de su sucursal
 * @param stock unidades disponibles
 */
public record Product(UUID id, Name name, Stock stock) {

    /**
     * Crea un producto nuevo con identidad recién generada.
     *
     * @param name  nombre comercial
     * @param stock existencias iniciales
     * @return el producto creado
     */
    public static Product create(Name name, Stock stock) {
        return new Product(UUID.randomUUID(), name, stock);
    }

    /** @return copia del producto con el nombre indicado */
    public Product withName(Name newName) {
        return new Product(id, newName, stock);
    }

    /** @return copia del producto con el stock indicado */
    public Product withStock(Stock newStock) {
        return new Product(id, name, newStock);
    }
}
