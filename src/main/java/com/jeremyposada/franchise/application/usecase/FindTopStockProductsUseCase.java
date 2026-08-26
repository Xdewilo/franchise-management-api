package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.TopStockProduct;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Criterio 7 — el producto con más stock de cada sucursal de una franquicia.
 *
 * <p>Antes de consultar se comprueba que la franquicia exista: sin esa
 * comprobación, un identificador inventado devolvería una lista vacía —
 * indistinguible de una franquicia real sin productos— en lugar de un 404.
 */
@Service
@RequiredArgsConstructor
public class FindTopStockProductsUseCase {

    private final FranchiseRepository repository;
    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @return un producto por sucursal; las sucursales sin catálogo no aparecen
     */
    public Flux<TopStockProduct> execute(UUID franchiseId) {
        return mutator.load(franchiseId)
                .flatMapMany(franchise -> repository.findTopStockProductPerBranch(franchise.id()));
    }
}
