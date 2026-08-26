package com.jeremyposada.franchise.infrastructure.persistence;

import java.util.UUID;

/**
 * Representación de un producto dentro de la columna JSONB.
 *
 * @param id    identidad del producto
 * @param name  nombre del producto
 * @param stock existencias
 */
public record ProductDocument(UUID id, String name, long stock) {
}
