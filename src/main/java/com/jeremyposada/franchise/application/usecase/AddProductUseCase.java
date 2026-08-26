package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Criterio 4 — incorpora un producto al catálogo de una sucursal.
 */
@Service
@RequiredArgsConstructor
public class AddProductUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchId    identidad de la sucursal
     * @param productName nombre del producto
     * @param stock       existencias iniciales; ausente equivale a cero
     * @return la franquicia con el producto añadido
     */
    public Mono<Franchise> execute(UUID franchiseId, UUID branchId, String productName, Long stock) {
        return Mono.fromCallable(() -> new ProductData(new Name(productName), Stock.of(stock)))
                .flatMap(data -> mutator.mutate(franchiseId,
                        franchise -> franchise.addProduct(branchId, data.name(), data.stock())));
    }

    private record ProductData(Name name, Stock stock) {}
}
