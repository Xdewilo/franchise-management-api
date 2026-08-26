package com.jeremyposada.franchise.domain.exception;

/**
 * Catálogo de fallos que el dominio sabe expresar.
 *
 * <p>El código viaja dentro de la excepción hasta el adaptador REST, que es el
 * único que decide cómo se traduce a HTTP. Así el dominio nombra <em>qué</em>
 * salió mal sin conocer códigos de estado ni nada del transporte.
 */
public enum DomainErrorCode {

    /** Un nombre llegó vacío o excede la longitud admitida. */
    INVALID_NAME,

    /** Se intentó fijar un stock negativo. */
    INVALID_STOCK,

    /** La franquicia solicitada no existe. */
    FRANCHISE_NOT_FOUND,

    /** La sucursal no pertenece a la franquicia indicada. */
    BRANCH_NOT_FOUND,

    /** El producto no pertenece a la sucursal indicada. */
    PRODUCT_NOT_FOUND,

    /** Ya existe otra franquicia con ese nombre. */
    DUPLICATE_FRANCHISE_NAME,

    /** La franquicia ya tiene una sucursal con ese nombre. */
    DUPLICATE_BRANCH_NAME,

    /** La sucursal ya oferta un producto con ese nombre. */
    DUPLICATE_PRODUCT_NAME,

    /** Otra petición modificó la franquicia mientras se procesaba ésta. */
    CONCURRENT_MODIFICATION
}
