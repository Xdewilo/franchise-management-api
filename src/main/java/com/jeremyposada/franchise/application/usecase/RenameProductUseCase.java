package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Punto extra — actualiza el nombre de un producto.
 */
@Service
@RequiredArgsConstructor
public class RenameProductUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchId    identidad de la sucursal
     * @param productId   identidad del producto
     * @param newName     nombre nuevo
     * @return la franquicia con el producto renombrado
     */
    public Mono<Franchise> execute(UUID franchiseId, UUID branchId, UUID productId, String newName) {
        return Mono.fromCallable(() -> new Name(newName))
                .flatMap(name -> mutator.mutate(franchiseId,
                        franchise -> franchise.renameProduct(branchId, productId, name)));
    }
}
