package com.jeremyposada.franchise.domain.exception;

/**
 * Raíz de los fallos de negocio.
 *
 * <p>Sus tres subclases —{@link ValidationException}, {@link NotFoundException}
 * y {@link ConflictException}— agrupan los fallos por naturaleza, no por caso
 * concreto: el caso concreto lo aporta el {@link DomainErrorCode}. Eso evita
 * una clase de excepción por regla y mantiene la traducción a HTTP en una
 * única tabla legible.
 */
public abstract class DomainException extends RuntimeException {

    private final DomainErrorCode code;

    protected DomainException(DomainErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /** @return código que identifica la regla de negocio incumplida */
    public DomainErrorCode getCode() {
        return code;
    }
}
