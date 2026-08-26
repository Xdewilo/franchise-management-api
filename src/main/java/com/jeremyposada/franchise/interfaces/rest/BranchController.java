package com.jeremyposada.franchise.interfaces.rest;

import com.jeremyposada.franchise.application.usecase.AddBranchUseCase;
import com.jeremyposada.franchise.application.usecase.RenameBranchUseCase;
import com.jeremyposada.franchise.interfaces.rest.dto.CreateBranchRequest;
import com.jeremyposada.franchise.interfaces.rest.dto.FranchiseResponse;
import com.jeremyposada.franchise.interfaces.rest.dto.UpdateNameRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Operaciones sobre las sucursales de una franquicia.
 *
 * <p>Todas devuelven la franquicia completa: la sucursal no existe fuera de su
 * agregado, y devolver la raíz deja al cliente con el estado consistente tras
 * la operación.
 */
@RestController
@RequestMapping("/api/v1/franchises/{franchiseId}/branches")
@RequiredArgsConstructor
@Tag(name = "Sucursales", description = "Alta y renombrado de sucursales")
public class BranchController {

    private final AddBranchUseCase addBranch;
    private final RenameBranchUseCase renameBranch;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una sucursal en la franquicia",
            description = "Criterio 3. El nombre debe ser único dentro de la franquicia.")
    @ApiResponse(responseCode = "201", description = "Sucursal creada")
    @ApiResponse(responseCode = "404", description = "La franquicia no existe")
    @ApiResponse(responseCode = "409", description = "Ya hay una sucursal con ese nombre")
    public Mono<FranchiseResponse> add(
            @PathVariable UUID franchiseId,
            @Valid @RequestBody CreateBranchRequest request) {
        return addBranch.execute(franchiseId, request.name()).map(FranchiseResponse::from);
    }

    @PatchMapping("/{branchId}/name")
    @Operation(summary = "Actualiza el nombre de una sucursal",
            description = "Punto extra del enunciado.")
    @ApiResponse(responseCode = "404", description = "La franquicia o la sucursal no existen")
    @ApiResponse(responseCode = "409", description = "Otra sucursal ya usa ese nombre")
    public Mono<FranchiseResponse> rename(
            @PathVariable UUID franchiseId,
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateNameRequest request) {
        return renameBranch.execute(franchiseId, branchId, request.name()).map(FranchiseResponse::from);
    }
}
