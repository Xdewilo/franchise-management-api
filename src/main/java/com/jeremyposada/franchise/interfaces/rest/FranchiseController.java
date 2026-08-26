package com.jeremyposada.franchise.interfaces.rest;

import com.jeremyposada.franchise.application.usecase.CreateFranchiseUseCase;
import com.jeremyposada.franchise.application.usecase.FindTopStockProductsUseCase;
import com.jeremyposada.franchise.application.usecase.GetFranchiseUseCase;
import com.jeremyposada.franchise.application.usecase.ListFranchisesUseCase;
import com.jeremyposada.franchise.application.usecase.RenameFranchiseUseCase;
import com.jeremyposada.franchise.interfaces.rest.dto.CreateFranchiseRequest;
import com.jeremyposada.franchise.interfaces.rest.dto.FranchiseResponse;
import com.jeremyposada.franchise.interfaces.rest.dto.PageResponse;
import com.jeremyposada.franchise.interfaces.rest.dto.TopStockProductResponse;
import com.jeremyposada.franchise.interfaces.rest.dto.UpdateNameRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Operaciones sobre franquicias.
 *
 * <p>El controlador se limita a traducir HTTP a casos de uso y de vuelta: no
 * contiene reglas de negocio, y por eso cada método cabe en dos líneas.
 */
@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@Tag(name = "Franquicias", description = "Alta, consulta y renombrado de franquicias")
public class FranchiseController {

    private final CreateFranchiseUseCase createFranchise;
    private final RenameFranchiseUseCase renameFranchise;
    private final GetFranchiseUseCase getFranchise;
    private final ListFranchisesUseCase listFranchises;
    private final FindTopStockProductsUseCase findTopStockProducts;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una franquicia",
            description = "Criterio 2. El nombre debe ser único en todo el sistema.")
    @ApiResponse(responseCode = "201", description = "Franquicia registrada")
    @ApiResponse(responseCode = "400", description = "El nombre es inválido")
    @ApiResponse(responseCode = "409", description = "El nombre ya está tomado")
    public Mono<FranchiseResponse> create(@Valid @RequestBody CreateFranchiseRequest request) {
        return createFranchise.execute(request.name()).map(FranchiseResponse::from);
    }

    @GetMapping
    @Operation(summary = "Lista las franquicias registradas",
            description = "Resultado paginado y ordenado por nombre.")
    public Mono<PageResponse<FranchiseResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return listFranchises.execute(page, size)
                .map(result -> PageResponse.from(result, FranchiseResponse::from));
    }

    @GetMapping("/{franchiseId}")
    @Operation(summary = "Consulta una franquicia con su árbol completo",
            description = "Permite verificar el resultado de cualquier operación de escritura.")
    @ApiResponse(responseCode = "404", description = "La franquicia no existe")
    public Mono<FranchiseResponse> get(@PathVariable UUID franchiseId) {
        return getFranchise.execute(franchiseId).map(FranchiseResponse::from);
    }

    @PatchMapping("/{franchiseId}/name")
    @Operation(summary = "Actualiza el nombre de una franquicia",
            description = "Punto extra del enunciado.")
    @ApiResponse(responseCode = "404", description = "La franquicia no existe")
    @ApiResponse(responseCode = "409", description = "El nombre ya está tomado por otra franquicia")
    public Mono<FranchiseResponse> rename(
            @PathVariable UUID franchiseId,
            @Valid @RequestBody UpdateNameRequest request) {
        return renameFranchise.execute(franchiseId, request.name()).map(FranchiseResponse::from);
    }

    @GetMapping("/{franchiseId}/branches/top-stock-products")
    @Operation(summary = "Producto con más stock por sucursal",
            description = "Criterio 7. Devuelve un producto por sucursal indicando a cuál pertenece. "
                    + "Las sucursales sin productos no aparecen en el resultado.")
    @ApiResponse(responseCode = "404", description = "La franquicia no existe")
    public Flux<TopStockProductResponse> topStockProducts(@PathVariable UUID franchiseId) {
        return findTopStockProducts.execute(franchiseId).map(TopStockProductResponse::from);
    }
}
