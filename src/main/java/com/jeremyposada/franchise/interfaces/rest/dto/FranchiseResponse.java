package com.jeremyposada.franchise.interfaces.rest.dto;

import com.jeremyposada.franchise.domain.model.Franchise;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Franquicia con su árbol completo.
 *
 * <p>Las operaciones de escritura devuelven este objeto para que el cliente
 * confirme el estado resultante sin necesitar una consulta adicional. La
 * versión se expone porque es la que permite razonar sobre concurrencia desde
 * fuera del sistema.
 *
 * @param id       identidad de la franquicia
 * @param name     nombre comercial
 * @param branches sucursales
 * @param version  versión del agregado tras la operación
 */
@Schema(description = "Franquicia con sus sucursales y productos")
public record FranchiseResponse(UUID id, String name, List<BranchResponse> branches, Long version) {

    /**
     * @param franchise agregado del dominio
     * @return su representación pública
     */
    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(
                franchise.id(),
                franchise.name().value(),
                franchise.branches().stream().map(BranchResponse::from).toList(),
                franchise.version());
    }
}
