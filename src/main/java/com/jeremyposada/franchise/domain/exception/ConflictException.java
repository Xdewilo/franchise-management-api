package com.jeremyposada.franchise.domain.exception;

/**
 * La operación choca con el estado actual: un nombre ya tomado o una
 * modificación concurrente.
 */
public class ConflictException extends DomainException {

    public ConflictException(DomainErrorCode code, String message) {
        super(code, message);
    }

    public static ConflictException duplicateFranchiseName(String name) {
        return new ConflictException(
                DomainErrorCode.DUPLICATE_FRANCHISE_NAME,
                "Ya existe una franquicia llamada '" + name + "'");
    }

    public static ConflictException duplicateBranchName(String name) {
        return new ConflictException(
                DomainErrorCode.DUPLICATE_BRANCH_NAME,
                "La franquicia ya tiene una sucursal llamada '" + name + "'");
    }

    public static ConflictException duplicateProductName(String name) {
        return new ConflictException(
                DomainErrorCode.DUPLICATE_PRODUCT_NAME,
                "La sucursal ya oferta un producto llamado '" + name + "'");
    }

    public static ConflictException concurrentModification() {
        return new ConflictException(
                DomainErrorCode.CONCURRENT_MODIFICATION,
                "La franquicia fue modificada por otra petición. Reintente la operación.");
    }
}
