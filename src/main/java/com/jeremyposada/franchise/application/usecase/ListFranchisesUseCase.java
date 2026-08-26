package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.application.PagedResult;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Lista las franquicias registradas.
 *
 * <p>Va paginado desde el primer día: un listado sin límite es una consulta
 * que funciona con datos de prueba y deja de funcionar en producción.
 */
@Service
@RequiredArgsConstructor
public class ListFranchisesUseCase {

    /** Tamaño de página por defecto cuando el cliente no lo indica. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Techo de página, para que un cliente no pueda pedir la tabla entera. */
    public static final int MAX_PAGE_SIZE = 100;

    private final FranchiseRepository repository;

    /**
     * @param page número de página solicitado, base cero
     * @param size tamaño de página solicitado
     * @return la página de franquicias junto con el total existente
     */
    public Mono<PagedResult<Franchise>> execute(Integer page, Integer size) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = Math.clamp(size == null ? DEFAULT_PAGE_SIZE : size, 1, MAX_PAGE_SIZE);

        return repository.findAll(safePage, safeSize)
                .collectList()
                .zipWith(repository.count())
                .map(tuple -> new PagedResult<>(tuple.getT1(), safePage, safeSize, tuple.getT2()));
    }
}
