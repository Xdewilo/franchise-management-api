package com.jeremyposada.franchise.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

/**
 * Representación de una sucursal tal como se serializa dentro de la columna
 * JSONB.
 *
 * <p>Es deliberadamente distinta del modelo de dominio: usa tipos planos en
 * lugar de value objects para que el JSON almacenado sea legible
 * ({@code {"id": "...", "name": "Sucursal Norte"}}) y no arrastre la forma
 * interna de {@code Name} o {@code Stock}. Ese desacoplamiento permite además
 * evolucionar el dominio sin reescribir los datos ya guardados.
 *
 * @param id       identidad de la sucursal
 * @param name     nombre de la sucursal
 * @param products productos ofertados
 */
public record BranchDocument(UUID id, String name, List<ProductDocument> products) {
}
