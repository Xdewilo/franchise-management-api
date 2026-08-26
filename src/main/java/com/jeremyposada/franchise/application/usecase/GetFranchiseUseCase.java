package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.model.Franchise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Consulta el árbol completo de una franquicia.
 *
 * <p>No figura entre los criterios del enunciado, pero es lo que permite
 * verificar el resultado de cualquier operación de escritura sin abrir la base
 * de datos.
 */
@Service
@RequiredArgsConstructor
public class GetFranchiseUseCase {

    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @return el agregado completo
     */
    public Mono<Franchise> execute(UUID franchiseId) {
        return mutator.load(franchiseId);
    }
}
