package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.ValidationException;

import java.util.Locale;

/**
 * Nombre comercial de una franquicia, sucursal o producto.
 *
 * <p>Existe como value object —y no como {@code String} suelto— por dos
 * razones concretas:
 *
 * <ul>
 *   <li>La invariante (no vacío, longitud acotada) se valida en un único sitio
 *       y es imposible construir un nombre inválido.</li>
 *   <li>La comparación de negocio ignora mayúsculas y espacios sobrantes, que
 *       es lo que hace que "Vive Fresh" y " vive fresh " se traten como el
 *       mismo nombre al detectar duplicados.</li>
 * </ul>
 *
 * @param value texto normalizado (sin espacios en los extremos)
 */
public record Name(String value) {

    /** Tope alineado con lo que admite la interfaz de usuario. */
    public static final int MAX_LENGTH = 120;

    public Name {
        if (value == null || value.isBlank()) {
            throw ValidationException.invalidName("El nombre no puede estar vacío");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw ValidationException.invalidName(
                    "El nombre no puede superar " + MAX_LENGTH + " caracteres, se recibieron: " + value.length());
        }
    }

    /**
     * Compara por igualdad de negocio: sin distinguir mayúsculas ni acentos de
     * espaciado. Es el criterio que usan las reglas de unicidad del agregado.
     *
     * @param other nombre a comparar
     * @return {@code true} si ambos nombres designan lo mismo para el negocio
     */
    public boolean matches(Name other) {
        return other != null && normalized().equals(other.normalized());
    }

    private String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
