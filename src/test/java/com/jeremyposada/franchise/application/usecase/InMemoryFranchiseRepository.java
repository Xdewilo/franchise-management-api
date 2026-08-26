package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.model.Branch;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.Product;
import com.jeremyposada.franchise.domain.model.TopStockProduct;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación en memoria del puerto de persistencia, para probar los casos
 * de uso sin base de datos.
 *
 * <p>Que este doble quepa en unas pocas líneas es la ventaja concreta de que
 * el puerto lo defina el dominio: los tests de aplicación corren en
 * milisegundos y sin Docker.
 *
 * <p>Reproduce también el bloqueo optimista, que es lo que permite verificar
 * el reintento de {@link FranchiseMutator} de forma determinista.
 */
class InMemoryFranchiseRepository implements FranchiseRepository {

    private final Map<UUID, Franchise> store = new ConcurrentHashMap<>();

    /** Número de escrituras que deben fallar por conflicto antes de tener éxito. */
    private final AtomicInteger scheduledConflicts = new AtomicInteger();

    /** Escrituras aceptadas; permite comprobar cuántos intentos hubo. */
    private final AtomicInteger acceptedWrites = new AtomicInteger();

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.fromCallable(() -> {
            if (scheduledConflicts.getAndUpdate(pending -> Math.max(0, pending - 1)) > 0) {
                throw ConflictException.concurrentModification();
            }
            long nextVersion = franchise.version() == null ? 0L : franchise.version() + 1;
            Franchise persisted = new Franchise(
                    franchise.id(), franchise.name(), franchise.branches(), nextVersion);
            store.put(persisted.id(), persisted);
            acceptedWrites.incrementAndGet();
            return persisted;
        });
    }

    @Override
    public Mono<Franchise> findById(UUID franchiseId) {
        return Mono.justOrEmpty(store.get(franchiseId));
    }

    @Override
    public Mono<Boolean> existsByName(Name name, UUID excluding) {
        return Mono.just(store.values().stream()
                .filter(franchise -> !franchise.id().equals(excluding))
                .anyMatch(franchise -> franchise.name().matches(name)));
    }

    @Override
    public Flux<Franchise> findAll(int page, int size) {
        return Flux.fromStream(store.values().stream()
                .sorted(Comparator.comparing(franchise -> franchise.name().value()))
                .skip((long) page * size)
                .limit(size));
    }

    @Override
    public Mono<Long> count() {
        return Mono.just((long) store.size());
    }

    @Override
    public Flux<TopStockProduct> findTopStockProductPerBranch(UUID franchiseId) {
        Franchise franchise = store.get(franchiseId);
        if (franchise == null) {
            return Flux.empty();
        }
        return Flux.fromStream(franchise.branches().stream()
                .flatMap(branch -> branch.products().stream()
                        .max(Comparator.comparingLong((Product product) -> product.stock().value())
                                .thenComparing(product -> product.name().value()))
                        .map(product -> toProjection(branch, product))
                        .stream()));
    }

    private static TopStockProduct toProjection(Branch branch, Product product) {
        return new TopStockProduct(
                branch.id(), branch.name().value(),
                product.id(), product.name().value(), product.stock().value());
    }

    // --- Utilidades de test -------------------------------------------------

    /** Guarda un agregado saltándose la simulación de conflictos. */
    Franchise seed(Franchise franchise) {
        Franchise persisted = new Franchise(
                franchise.id(), franchise.name(), franchise.branches(),
                franchise.version() == null ? 0L : franchise.version());
        store.put(persisted.id(), persisted);
        return persisted;
    }

    /** Programa las próximas {@code times} escrituras para que fallen por conflicto. */
    void failNextWritesWithConflict(int times) {
        scheduledConflicts.set(times);
    }

    /** @return número de escrituras que llegaron a persistirse */
    int acceptedWrites() {
        return acceptedWrites.get();
    }
}
