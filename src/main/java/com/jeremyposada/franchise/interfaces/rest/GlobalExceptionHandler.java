package com.jeremyposada.franchise.interfaces.rest;

import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas HTTP.
 *
 * <p>Es el único punto del sistema que conoce códigos de estado: el dominio
 * lanza errores nombrados por su regla de negocio y aquí se decide cómo se ven
 * desde fuera. Añadir una regla nueva no obliga a tocar los controladores.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Traducción de cada regla de negocio a su código HTTP. */
    private static final Map<DomainErrorCode, HttpStatus> STATUS_BY_CODE = Map.of(
            DomainErrorCode.INVALID_NAME, HttpStatus.BAD_REQUEST,
            DomainErrorCode.INVALID_STOCK, HttpStatus.BAD_REQUEST,
            DomainErrorCode.FRANCHISE_NOT_FOUND, HttpStatus.NOT_FOUND,
            DomainErrorCode.BRANCH_NOT_FOUND, HttpStatus.NOT_FOUND,
            DomainErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND,
            DomainErrorCode.DUPLICATE_FRANCHISE_NAME, HttpStatus.CONFLICT,
            DomainErrorCode.DUPLICATE_BRANCH_NAME, HttpStatus.CONFLICT,
            DomainErrorCode.DUPLICATE_PRODUCT_NAME, HttpStatus.CONFLICT,
            DomainErrorCode.CONCURRENT_MODIFICATION, HttpStatus.CONFLICT);

    /**
     * Fallos de negocio: nombre inválido, entidad inexistente o conflicto.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException error, ServerWebExchange exchange) {
        HttpStatus status = STATUS_BY_CODE.getOrDefault(error.getCode(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), error.getCode().name(), error.getMessage(), path(exchange)));
    }

    /**
     * Validación del cuerpo de la petición: se devuelven todos los campos
     * rechazados de una vez, en lugar de obligar a descubrirlos uno a uno.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidation(WebExchangeBindException error, ServerWebExchange exchange) {
        List<ApiError.FieldViolation> violations = error.getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldViolation(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "La petición contiene campos inválidos",
                path(exchange),
                Instant.now(),
                violations));
    }

    /**
     * Cuerpo ilegible o parámetro de ruta con formato incorrecto —por ejemplo,
     * un identificador que no es un UUID.
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleMalformedInput(ServerWebInputException error, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST",
                "La petición no tiene el formato esperado: " + error.getReason(),
                path(exchange)));
    }

    /**
     * Red de seguridad. Se registra la traza completa en el log pero al cliente
     * sólo se le devuelve un mensaje genérico: los detalles internos no deben
     * salir en una respuesta HTTP.
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleUnexpected(Throwable error, ServerWebExchange exchange) {
        log.error("Error no controlado en {}", path(exchange), error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado al procesar la petición",
                path(exchange)));
    }

    private static String path(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value();
    }
}
