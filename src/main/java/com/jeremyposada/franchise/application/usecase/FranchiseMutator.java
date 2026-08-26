package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.DomainException;
import com.jeremyposada.franchise.domain.exception.NotFoundException;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Colaborador que ejecuta el ciclo «cargar agregado → aplicar cambio →
 * guardar» que comparten todos los casos de uso de escritura.
 *
 * <p>Existe para que cada caso de uso quede reducido a la regla de negocio que
 * le es propia, y para concentrar en un solo sitio el reintento ante
 * modificaciones concurrentes.
 *
 * <p><b>Concurrencia.</b> Como el agregado se guarda completo, dos peticiones
 * simultáneas sobre la misma franquicia harían que la última pisara los
 * cambios de la primera. El bloqueo optimista lo impide: la segunda escritura
 * falla, y aquí se reintenta el ciclo completo —releyendo el estado ya
 * actualizado— antes de rendirse. Sólo si el conflicto persiste tras varios
 * intentos se propaga el error al cliente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FranchiseMutator {

    /** Reintentos ante conflicto optimista antes de devolver el error. */
    private static final int MAX_RETRIES = 3;

    private final FranchiseRepository repository;

    /**
     * Carga la franquicia, le aplica el cambio y persiste el resultado.
     *
     * @param franchiseId identidad de la franquicia
     * @param change      transformación a aplicar sobre el agregado
     * @return el agregado ya persistido
     */
    public Mono<Franchise> mutate(UUID franchiseId, UnaryOperator<Franchise> change) {
        return Mono.defer(() -> load(franchiseId)
                        .map(change)
                        .flatMap(repository::save))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(20))
                        .filter(FranchiseMutator::isConcurrentModification)
                        .doBeforeRetry(signal -> log.debug(
                                "Conflicto optimista sobre la franquicia {}, reintento {}",
                                franchiseId, signal.totalRetries() + 1))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    /**
     * Carga la franquicia o falla si no existe.
     *
     * @param franchiseId identidad de la franquicia
     * @return el agregado
     */
    public Mono<Franchise> load(UUID franchiseId) {
        return repository.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    private static boolean isConcurrentModification(Throwable error) {
        return error instanceof DomainException domainError
                && domainError.getCode() == DomainErrorCode.CONCURRENT_MODIFICATION;
    }
}
