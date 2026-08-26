package com.jeremyposada.franchise.interfaces.rest;

import com.jeremyposada.franchise.application.usecase.AddProductUseCase;
import com.jeremyposada.franchise.application.usecase.RemoveProductUseCase;
import com.jeremyposada.franchise.application.usecase.RenameProductUseCase;
import com.jeremyposada.franchise.application.usecase.UpdateProductStockUseCase;
import com.jeremyposada.franchise.interfaces.rest.dto.CreateProductRequest;
import com.jeremyposada.franchise.interfaces.rest.dto.FranchiseResponse;
import com.jeremyposada.franchise.interfaces.rest.dto.UpdateNameRequest;
import com.jeremyposada.franchise.interfaces.rest.dto.UpdateStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Operaciones sobre los productos de una sucursal.
 */
@RestController
@RequestMapping("/api/v1/franchises/{franchiseId}/branches/{branchId}/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Alta, baja, stock y renombrado de productos")
public class ProductController {

    private final AddProductUseCase addProduct;
    private final RemoveProductUseCase removeProduct;
    private final UpdateProductStockUseCase updateProductStock;
    private final RenameProductUseCase renameProduct;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Incorpora un producto al catálogo de la sucursal",
            description = "Criterio 4. El nombre debe ser único dentro de la sucursal.")
    @ApiResponse(responseCode = "201", description = "Producto agregado")
    @ApiResponse(responseCode = "404", description = "La franquicia o la sucursal no existen")
    @ApiResponse(responseCode = "409", description = "La sucursal ya oferta ese producto")
    public Mono<FranchiseResponse> add(
            @PathVariable UUID franchiseId,
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateProductRequest request) {
        return addProduct.execute(franchiseId, branchId, request.name(), request.stock())
                .map(FranchiseResponse::from);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Retira un producto del catálogo",
            description = "Criterio 5. Devuelve la franquicia ya sin el producto.")
    @ApiResponse(responseCode = "404", description = "La franquicia, la sucursal o el producto no existen")
    public Mono<FranchiseResponse> remove(
            @PathVariable UUID franchiseId,
            @PathVariable UUID branchId,
            @PathVariable UUID productId) {
        return removeProduct.execute(franchiseId, branchId, productId).map(FranchiseResponse::from);
    }

    @PatchMapping("/{productId}/stock")
    @Operation(summary = "Modifica el stock de un producto",
            description = "Criterio 6. El valor es absoluto, no un incremento, "
                    + "de modo que repetir la petición no altera el resultado.")
    @ApiResponse(responseCode = "400", description = "El stock es negativo")
    @ApiResponse(responseCode = "404", description = "La franquicia, la sucursal o el producto no existen")
    public Mono<FranchiseResponse> updateStock(
            @PathVariable UUID franchiseId,
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return updateProductStock.execute(franchiseId, branchId, productId, request.stock())
                .map(FranchiseResponse::from);
    }

    @PatchMapping("/{productId}/name")
    @Operation(summary = "Actualiza el nombre de un producto",
            description = "Punto extra del enunciado.")
    @ApiResponse(responseCode = "404", description = "La franquicia, la sucursal o el producto no existen")
    @ApiResponse(responseCode = "409", description = "La sucursal ya oferta otro producto con ese nombre")
    public Mono<FranchiseResponse> rename(
            @PathVariable UUID franchiseId,
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateNameRequest request) {
        return renameProduct.execute(franchiseId, branchId, productId, request.name())
                .map(FranchiseResponse::from);
    }
}
