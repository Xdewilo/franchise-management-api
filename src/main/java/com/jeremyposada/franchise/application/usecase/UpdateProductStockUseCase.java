package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Criterio 6 — fija las existencias de un producto.
 *
 * <p>La operación asigna el valor absoluto en lugar de sumar o restar: el
 * enunciado pide «modificar el stock», y una asignación es idempotente —
 * repetir la petición deja el mismo resultado, lo que la hace segura ante
 * reintentos de red.
 */
@Service
@RequiredArgsConstructor
public class UpdateProductStockUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchId    identidad de la sucursal
     * @param productId   identidad del producto
     * @param stock       existencias nuevas
     * @return la franquicia con el stock actualizado
     */
    public Mono<Franchise> execute(UUID franchiseId, UUID branchId, UUID productId, Long stock) {
        return Mono.fromCallable(() -> Stock.of(stock))
                .flatMap(newStock -> mutator.mutate(franchiseId,
                        franchise -> franchise.updateProductStock(branchId, productId, newStock)));
    }
}
