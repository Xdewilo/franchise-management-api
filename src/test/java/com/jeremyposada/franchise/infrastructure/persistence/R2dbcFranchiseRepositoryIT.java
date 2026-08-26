package com.jeremyposada.franchise.infrastructure.persistence;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.model.Branch;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.Stock;
import com.jeremyposada.franchise.domain.model.TopStockProduct;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Adaptador R2DBC sobre PostgreSQL")
class R2dbcFranchiseRepositoryIT extends PostgresTestSupport {

    @Autowired
    private FranchiseRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        databaseClient.sql("TRUNCATE TABLE franchises").fetch().rowsUpdated().block();
    }

    @Test
    @DisplayName("guarda el árbol completo y lo reconstruye idéntico")
    void roundTripsTheWholeAggregate() {
        Franchise saved = persistSampleFranchise();

        StepVerifier.create(repository.findById(saved.id()))
                .assertNext(loaded -> {
                    assertThat(loaded.name().value()).isEqualTo("Vive Fresh");
                    assertThat(loaded.branches()).hasSize(2);

                    Branch north = loaded.branches().stream()
                            .filter(branch -> branch.name().value().equals("Sucursal Norte"))
                            .findFirst().orElseThrow();
                    assertThat(north.products()).hasSize(2);
                    assertThat(north.products())
                            .extracting(product -> product.stock().value())
                            .containsExactlyInAnyOrder(25L, 80L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("incrementa la versión en cada escritura")
    void bumpsVersionOnEveryWrite() {
        Franchise created = repository.save(Franchise.create(new Name("Vive Fresh"))).block();
        assertThat(created.version()).isZero();

        Franchise updated = repository.save(created.addBranch(new Name("Sucursal Norte"))).block();

        assertThat(updated.version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("rechaza escribir sobre una versión ya superada")
    void rejectsStaleWrites() {
        Franchise created = repository.save(Franchise.create(new Name("Vive Fresh"))).block();
        // Simula dos peticiones concurrentes: ambas parten de la misma versión.
        Franchise firstWriter = created.addBranch(new Name("Sucursal Norte"));
        Franchise secondWriter = created.addBranch(new Name("Sucursal Sur"));

        repository.save(firstWriter).block();

        StepVerifier.create(repository.save(secondWriter))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ConflictException.class)
                        .extracting("code")
                        .isEqualTo(DomainErrorCode.CONCURRENT_MODIFICATION))
                .verify();
    }

    @Test
    @DisplayName("el índice único impide dos franquicias con el mismo nombre")
    void enforcesUniqueName() {
        repository.save(Franchise.create(new Name("Vive Fresh"))).block();

        StepVerifier.create(repository.save(Franchise.create(new Name("VIVE FRESH"))))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ConflictException.class)
                        .extracting("code")
                        .isEqualTo(DomainErrorCode.DUPLICATE_FRANCHISE_NAME))
                .verify();
    }

    @Test
    @DisplayName("detecta nombres tomados y excluye a la propia franquicia")
    void checksNameAvailability() {
        Franchise saved = repository.save(Franchise.create(new Name("Vive Fresh"))).block();

        StepVerifier.create(repository.existsByName(new Name("vive fresh"), null))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.existsByName(new Name("vive fresh"), saved.id()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("devuelve el producto de mayor stock de cada sucursal, ordenado por sucursal")
    void resolvesTopStockPerBranch() {
        Franchise saved = persistSampleFranchise();

        List<TopStockProduct> top = repository.findTopStockProductPerBranch(saved.id())
                .collectList().block();

        assertThat(top).hasSize(2);
        assertThat(top).extracting(TopStockProduct::branchName)
                .containsExactly("Sucursal Norte", "Sucursal Sur");
        assertThat(top.getFirst().productName()).isEqualTo("Jugo de mango");
        assertThat(top.getFirst().stock()).isEqualTo(80L);
        assertThat(top.getLast().productName()).isEqualTo("Café");
    }

    @Test
    @DisplayName("desempata por nombre cuando dos productos tienen el mismo stock")
    void breaksStockTiesDeterministically() {
        Franchise franchise = Franchise.create(new Name("Vive Fresh")).addBranch(new Name("Sucursal Única"));
        UUID branchId = franchise.branches().getFirst().id();
        franchise = franchise
                .addProduct(branchId, new Name("Zumo"), new Stock(50))
                .addProduct(branchId, new Name("Agua"), new Stock(50));
        Franchise saved = repository.save(franchise).block();

        StepVerifier.create(repository.findTopStockProductPerBranch(saved.id()))
                .assertNext(top -> assertThat(top.productName()).isEqualTo("Agua"))
                .verifyComplete();
    }

    @Test
    @DisplayName("omite del resultado las sucursales sin productos")
    void skipsBranchesWithoutProducts() {
        Franchise franchise = Franchise.create(new Name("Vive Fresh")).addBranch(new Name("Sucursal Vacía"));
        Franchise saved = repository.save(franchise).block();

        StepVerifier.create(repository.findTopStockProductPerBranch(saved.id()))
                .verifyComplete();
    }

    @Test
    @DisplayName("pagina el listado ordenado por nombre")
    void paginatesOrderedByName() {
        repository.save(Franchise.create(new Name("Charlie"))).block();
        repository.save(Franchise.create(new Name("Alfa"))).block();
        repository.save(Franchise.create(new Name("Bravo"))).block();

        StepVerifier.create(repository.findAll(0, 2).map(franchise -> franchise.name().value()))
                .expectNext("Alfa", "Bravo")
                .verifyComplete();

        StepVerifier.create(repository.findAll(1, 2).map(franchise -> franchise.name().value()))
                .expectNext("Charlie")
                .verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(3L)
                .verifyComplete();
    }

    /**
     * Franquicia de ejemplo con dos sucursales: la del norte con dos productos
     * (25 y 80 unidades) y la del sur con uno (10).
     */
    private Franchise persistSampleFranchise() {
        Franchise franchise = Franchise.create(new Name("Vive Fresh"))
                .addBranch(new Name("Sucursal Norte"))
                .addBranch(new Name("Sucursal Sur"));

        UUID north = franchise.branches().getFirst().id();
        UUID south = franchise.branches().getLast().id();

        franchise = franchise
                .addProduct(north, new Name("Arepa de huevo"), new Stock(25))
                .addProduct(north, new Name("Jugo de mango"), new Stock(80))
                .addProduct(south, new Name("Café"), new Stock(10));

        return repository.save(franchise).block();
    }
}
