package com.jeremyposada.franchise.interfaces.rest.dto;

import com.jeremyposada.franchise.domain.model.Branch;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Sucursal tal como se expone en el API.
 *
 * @param id       identidad de la sucursal
 * @param name     nombre de la sucursal
 * @param products catálogo ofertado
 */
@Schema(description = "Sucursal de una franquicia")
public record BranchResponse(UUID id, String name, List<ProductResponse> products) {

    /**
     * @param branch sucursal del dominio
     * @return su representación pública
     */
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.id(),
                branch.name().value(),
                branch.products().stream().map(ProductResponse::from).toList());
    }
}
