package com.jeremyposada.franchise.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Forma única de los errores del API.
 *
 * <p>Todas las respuestas de error comparten esta estructura, con el mismo
 * {@code code} legible que emite el dominio, para que un cliente pueda
 * reaccionar de forma programática sin analizar textos.
 *
 * @param status    código HTTP
 * @param code      identificador estable del error
 * @param message   descripción legible
 * @param path      ruta que originó el error
 * @param timestamp instante en que se produjo
 * @param details   errores de campo, cuando el fallo es de validación
 */
@Schema(description = "Error devuelto por el API")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> details
) {

    /**
     * Detalle de un campo que no superó la validación.
     *
     * @param field   nombre del campo
     * @param message motivo del rechazo
     */
    @Schema(description = "Campo que no superó la validación")
    public record FieldViolation(String field, String message) {}

    /**
     * @param status  código HTTP
     * @param code    identificador del error
     * @param message descripción legible
     * @param path    ruta que lo originó
     * @return el error sin detalles de campo
     */
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, path, Instant.now(), List.of());
    }
}
