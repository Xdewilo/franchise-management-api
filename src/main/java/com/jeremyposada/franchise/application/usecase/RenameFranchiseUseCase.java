package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Punto extra — actualiza el nombre de una franquicia.
 *
 * <p>La unicidad se comprueba excluyendo a la propia franquicia, para que
 * renombrarla a su nombre actual no se interprete como un choque.
 */
@Service
@RequiredArgsConstructor
public class RenameFranchiseUseCase {

    private final FranchiseRepository repository;
    private final FranchiseMutator mutator;

    /**
     * @param franchiseId identidad de la franquicia
     * @param newName     nombre nuevo
     * @return la franquicia renombrada
     */
    public Mono<Franchise> execute(UUID franchiseId, String newName) {
        return Mono.fromCallable(() -> new Name(newName))
                .flatMap(name -> repository.existsByName(name, franchiseId)
                        .flatMap(taken -> taken
                                ? Mono.<Franchise>error(ConflictException.duplicateFranchiseName(name.value()))
                                : mutator.mutate(franchiseId, franchise -> franchise.rename(name))));
    }
}
