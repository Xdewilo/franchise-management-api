package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Sucursal de una franquicia, con el catálogo de productos que oferta.
 *
 * <p>Entidad interna del agregado {@link Franchise}. Guarda la invariante que
 * le corresponde —no puede ofertar dos productos con el mismo nombre— y delega
 * en {@link Product} las reglas propias del producto.
 *
 * @param id       identidad estable de la sucursal dentro del agregado
 * @param name     nombre, único dentro de su franquicia
 * @param products productos ofertados; lista inmutable
 */
public record Branch(UUID id, Name name, List<Product> products) {

    public Branch {
        products = List.copyOf(products);
    }

    /**
     * Crea una sucursal nueva, sin productos.
     *
     * @param name nombre de la sucursal
     * @return la sucursal creada
     */
    public static Branch create(Name name) {
        return new Branch(UUID.randomUUID(), name, List.of());
    }

    /** @return copia de la sucursal con el nombre indicado */
    public Branch withName(Name newName) {
        return new Branch(id, newName, products);
    }

    /**
     * Incorpora un producto al catálogo.
     *
     * @param productName nombre del producto
     * @param stock       existencias iniciales
     * @return copia de la sucursal con el producto añadido
     * @throws ConflictException si ya oferta un producto con ese nombre
     */
    public Branch addProduct(Name productName, Stock stock) {
        if (hasProductNamed(productName, null)) {
            throw ConflictException.duplicateProductName(productName.value());
        }
        return new Branch(id, name, append(products, Product.create(productName, stock)));
    }

    /**
     * Retira un producto del catálogo.
     *
     * @param productId identidad del producto
     * @return copia de la sucursal sin ese producto
     * @throws NotFoundException si el producto no pertenece a esta sucursal
     */
    public Branch removeProduct(UUID productId) {
        requireProduct(productId);
        return new Branch(id, name, products.stream()
                .filter(product -> !product.id().equals(productId))
                .toList());
    }

    /**
     * Renombra un producto del catálogo.
     *
     * @param productId identidad del producto
     * @param newName   nombre nuevo
     * @return copia de la sucursal con el producto renombrado
     * @throws NotFoundException si el producto no pertenece a esta sucursal
     * @throws ConflictException si otro producto ya usa ese nombre
     */
    public Branch renameProduct(UUID productId, Name newName) {
        requireProduct(productId);
        if (hasProductNamed(newName, productId)) {
            throw ConflictException.duplicateProductName(newName.value());
        }
        return replaceProduct(productId, product -> product.withName(newName));
    }

    /**
     * Ajusta las existencias de un producto.
     *
     * @param productId identidad del producto
     * @param newStock  existencias nuevas
     * @return copia de la sucursal con el stock actualizado
     * @throws NotFoundException si el producto no pertenece a esta sucursal
     */
    public Branch updateProductStock(UUID productId, Stock newStock) {
        requireProduct(productId);
        return replaceProduct(productId, product -> product.withStock(newStock));
    }

    /**
     * Busca un producto por identidad.
     *
     * @param productId identidad del producto
     * @return el producto
     * @throws NotFoundException si no pertenece a esta sucursal
     */
    public Product requireProduct(UUID productId) {
        return products.stream()
                .filter(product -> product.id().equals(productId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.product(productId));
    }

    private Branch replaceProduct(UUID productId, java.util.function.UnaryOperator<Product> change) {
        return new Branch(id, name, products.stream()
                .map(product -> product.id().equals(productId) ? change.apply(product) : product)
                .toList());
    }

    /**
     * @param candidate nombre a verificar
     * @param excluding producto que se excluye de la comparación (el que se
     *                  está renombrando), o {@code null} para no excluir ninguno
     */
    private boolean hasProductNamed(Name candidate, UUID excluding) {
        return products.stream()
                .filter(product -> !product.id().equals(excluding))
                .anyMatch(product -> product.name().matches(candidate));
    }

    private static <T> List<T> append(List<T> source, T element) {
        return java.util.stream.Stream.concat(source.stream(), java.util.stream.Stream.of(element)).toList();
    }
}
