package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Criterio 2 — registra una franquicia nueva.
 *
 * <p>El nombre se verifica antes de insertar para poder devolver un conflicto
 * explicativo. La condición de carrera que deja abierta esa verificación la
 * cubre el índice único de la tabla, que el adaptador traduce al mismo error.
 */
@Service
@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final FranchiseRepository repository;

    /**
     * @param name nombre comercial de la franquicia
     * @return la franquicia creada
     */
    public Mono<Franchise> execute(String name) {
        return Mono.fromCallable(() -> new Name(name))
                .flatMap(franchiseName -> repository.existsByName(franchiseName, null)
                        .flatMap(taken -> taken
                                ? Mono.<Franchise>error(ConflictException.duplicateFranchiseName(franchiseName.value()))
                                : repository.save(Franchise.create(franchiseName))));
    }
}
