package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Criterio 5 — retira un producto del catálogo de una sucursal.
 */
@Service
@RequiredArgsConstructor
public class RemoveProductUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchId    identidad de la sucursal
     * @param productId   identidad del producto
     * @return la franquicia sin ese producto
     */
    public Mono<Franchise> execute(UUID franchiseId, UUID branchId, UUID productId) {
        return mutator.mutate(franchiseId, franchise -> franchise.removeProduct(branchId, productId));
    }
}
