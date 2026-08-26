package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Criterio 3 — abre una sucursal en una franquicia existente.
 */
@Service
@RequiredArgsConstructor
public class AddBranchUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchName  nombre de la sucursal
     * @return la franquicia con la sucursal añadida
     */
    public Mono<Franchise> execute(UUID franchiseId, String branchName) {
        return Mono.fromCallable(() -> new Name(branchName))
                .flatMap(name -> mutator.mutate(franchiseId, franchise -> franchise.addBranch(name)));
    }
}
