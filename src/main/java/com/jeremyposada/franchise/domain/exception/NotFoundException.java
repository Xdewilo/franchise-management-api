package com.jeremyposada.franchise.domain.exception;

import java.util.UUID;

/**
 * La entidad referenciada no existe dentro del ámbito consultado.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(DomainErrorCode code, String message) {
        super(code, message);
    }

    public static NotFoundException franchise(UUID franchiseId) {
        return new NotFoundException(
                DomainErrorCode.FRANCHISE_NOT_FOUND,
                "No existe la franquicia " + franchiseId);
    }

    public static NotFoundException branch(UUID branchId) {
        return new NotFoundException(
                DomainErrorCode.BRANCH_NOT_FOUND,
                "La franquicia no tiene la sucursal " + branchId);
    }

    public static NotFoundException product(UUID productId) {
        return new NotFoundException(
                DomainErrorCode.PRODUCT_NOT_FOUND,
                "La sucursal no oferta el producto " + productId);
    }
}
