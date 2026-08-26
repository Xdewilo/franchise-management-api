package com.jeremyposada.franchise.domain.exception;

/**
 * Un valor no satisface la invariante que lo define.
 *
 * <p>Se lanza desde los value objects, antes de que el dato llegue a formar
 * parte del agregado.
 */
public class ValidationException extends DomainException {

    public ValidationException(DomainErrorCode code, String message) {
        super(code, message);
    }

    /** El nombre está vacío, en blanco o excede el máximo permitido. */
    public static ValidationException invalidName(String detail) {
        return new ValidationException(DomainErrorCode.INVALID_NAME, detail);
    }

    /** El stock solicitado es negativo. */
    public static ValidationException negativeStock(long value) {
        return new ValidationException(
                DomainErrorCode.INVALID_STOCK,
                "El stock no puede ser negativo, se recibió: " + value);
    }
}
