package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Punto extra — actualiza el nombre de una sucursal.
 */
@Service
@RequiredArgsConstructor
public class RenameBranchUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param branchId    identidad de la sucursal
     * @param newName     nombre nuevo
     * @return la franquicia con la sucursal renombrada
     */
    public Mono<Franchise> execute(UUID franchiseId, UUID branchId, String newName) {
        return Mono.fromCallable(() -> new Name(newName))
                .flatMap(name -> mutator.mutate(franchiseId, franchise -> franchise.renameBranch(branchId, name)));
    }
}
