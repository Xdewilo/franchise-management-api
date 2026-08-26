package com.jeremyposada.franchise.application.usecase;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.NotFoundException;
import com.jeremyposada.franchise.domain.exception.ValidationException;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.TopStockProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Casos de uso de franquicias")
class FranchiseUseCasesTest {

    private InMemoryFranchiseRepository repository;
    private FranchiseMutator mutator;

    private CreateFranchiseUseCase createFranchise;
    private RenameFranchiseUseCase renameFranchise;
    private AddBranchUseCase addBranch;
    private AddProductUseCase addProduct;
    private RemoveProductUseCase removeProduct;
    private UpdateProductStockUseCase updateStock;
    private RenameProductUseCase renameProduct;
    private ListFranchisesUseCase listFranchises;
    private FindTopStockProductsUseCase findTopStock;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFranchiseRepository();
        mutator = new FranchiseMutator(repository);

        createFranchise = new CreateFranchiseUseCase(repository);
        renameFranchise = new RenameFranchiseUseCase(repository, mutator);
        addBranch = new AddBranchUseCase(mutator);
        addProduct = new AddProductUseCase(mutator);
        removeProduct = new RemoveProductUseCase(mutator);
        updateStock = new UpdateProductStockUseCase(mutator);
        renameProduct = new RenameProductUseCase(mutator);
        listFranchises = new ListFranchisesUseCase(repository);
        findTopStock = new FindTopStockProductsUseCase(repository, mutator);
    }

    @Nested
    @DisplayName("Creación de franquicias")
    class Creation {

        @Test
        @DisplayName("registra la franquicia y devuelve la versión inicial")
        void createsFranchise() {
            StepVerifier.create(createFranchise.execute("Vive Fresh"))
                    .assertNext(franchise -> {
                        assertThat(franchise.name().value()).isEqualTo("Vive Fresh");
                        assertThat(franchise.version()).isZero();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("rechaza un nombre ya registrado")
        void rejectsDuplicateName() {
            createFranchise.execute("Vive Fresh").block();

            StepVerifier.create(createFranchise.execute("vive fresh"))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOf(ConflictException.class)
                            .extracting("code")
                            .isEqualTo(DomainErrorCode.DUPLICATE_FRANCHISE_NAME))
                    .verify();
        }

        @Test
        @DisplayName("propaga la validación del nombre como señal de error")
        void propagatesNameValidation() {
            StepVerifier.create(createFranchise.execute("   "))
                    .expectError(ValidationException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Operaciones sobre el árbol")
    class TreeOperations {

        @Test
        @DisplayName("añade sucursal y producto, y actualiza el stock")
        void walksTheHappyPath() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();
            franchise = addBranch.execute(franchise.id(), "Sucursal Norte").block();
            UUID branchId = franchise.branches().getFirst().id();

            franchise = addProduct.execute(franchise.id(), branchId, "Arepa de huevo", 25L).block();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            StepVerifier.create(updateStock.execute(franchise.id(), branchId, productId, 40L))
                    .assertNext(updated -> assertThat(
                            updated.requireBranch(branchId).requireProduct(productId).stock().value())
                            .isEqualTo(40L))
                    .verifyComplete();
        }

        @Test
        @DisplayName("elimina un producto del catálogo")
        void removesProduct() {
            Franchise franchise = franchiseWithProduct("Arepa de huevo", 25L);
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            StepVerifier.create(removeProduct.execute(franchise.id(), branchId, productId))
                    .assertNext(updated -> assertThat(updated.requireBranch(branchId).products()).isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("renombra un producto existente")
        void renamesProduct() {
            Franchise franchise = franchiseWithProduct("Arepa de huevo", 25L);
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            StepVerifier.create(renameProduct.execute(franchise.id(), branchId, productId, "Arepa de queso"))
                    .assertNext(updated -> assertThat(
                            updated.requireBranch(branchId).requireProduct(productId).name().value())
                            .isEqualTo("Arepa de queso"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("rechaza un stock negativo sin tocar el repositorio")
        void rejectsNegativeStock() {
            Franchise franchise = franchiseWithProduct("Arepa de huevo", 25L);
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();
            int writesBefore = repository.acceptedWrites();

            StepVerifier.create(updateStock.execute(franchise.id(), branchId, productId, -1L))
                    .expectError(ValidationException.class)
                    .verify();

            assertThat(repository.acceptedWrites()).isEqualTo(writesBefore);
        }

        @Test
        @DisplayName("falla si la franquicia no existe")
        void failsOnUnknownFranchise() {
            StepVerifier.create(addBranch.execute(UUID.randomUUID(), "Sucursal Norte"))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOf(NotFoundException.class)
                            .extracting("code")
                            .isEqualTo(DomainErrorCode.FRANCHISE_NOT_FOUND))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Renombrado de franquicia")
    class Renaming {

        @Test
        @DisplayName("admite renombrarla a su propio nombre")
        void allowsRenamingToItsOwnName() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();

            StepVerifier.create(renameFranchise.execute(franchise.id(), "Vive Fresh"))
                    .assertNext(updated -> assertThat(updated.name().value()).isEqualTo("Vive Fresh"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("rechaza el nombre de otra franquicia")
        void rejectsAnotherFranchiseName() {
            createFranchise.execute("Vive Fresh").block();
            Franchise other = createFranchise.execute("Vive Salud").block();

            StepVerifier.create(renameFranchise.execute(other.id(), "Vive Fresh"))
                    .expectError(ConflictException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Concurrencia")
    class Concurrency {

        @Test
        @DisplayName("reintenta cuando otra petición modificó la franquicia")
        void retriesOnConcurrentModification() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();
            repository.failNextWritesWithConflict(2);

            StepVerifier.create(addBranch.execute(franchise.id(), "Sucursal Norte"))
                    .assertNext(updated -> assertThat(updated.branches()).hasSize(1))
                    .verifyComplete();
        }

        @Test
        @DisplayName("se rinde y propaga el conflicto si el choque persiste")
        void givesUpAfterExhaustingRetries() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();
            repository.failNextWritesWithConflict(99);

            StepVerifier.create(addBranch.execute(franchise.id(), "Sucursal Norte"))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOf(ConflictException.class)
                            .extracting("code")
                            .isEqualTo(DomainErrorCode.CONCURRENT_MODIFICATION))
                    .verify();
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("acota el tamaño de página al máximo permitido")
        void clampsPageSize() {
            createFranchise.execute("Vive Fresh").block();

            StepVerifier.create(listFranchises.execute(-3, 5_000))
                    .assertNext(result -> {
                        assertThat(result.page()).isZero();
                        assertThat(result.size()).isEqualTo(ListFranchisesUseCase.MAX_PAGE_SIZE);
                        assertThat(result.totalItems()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("devuelve el producto de mayor stock de cada sucursal")
        void returnsTopStockPerBranch() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();
            franchise = addBranch.execute(franchise.id(), "Sucursal Norte").block();
            franchise = addBranch.execute(franchise.id(), "Sucursal Sur").block();
            UUID north = franchise.branches().getFirst().id();
            UUID south = franchise.branches().getLast().id();

            addProduct.execute(franchise.id(), north, "Arepa de huevo", 25L).block();
            addProduct.execute(franchise.id(), north, "Jugo de mango", 80L).block();
            addProduct.execute(franchise.id(), south, "Café", 10L).block();

            List<TopStockProduct> top = findTopStock.execute(franchise.id()).collectList().block();

            assertThat(top).hasSize(2);
            assertThat(top).extracting(TopStockProduct::productName)
                    .containsExactlyInAnyOrder("Jugo de mango", "Café");
            assertThat(top).extracting(TopStockProduct::branchName)
                    .containsExactlyInAnyOrder("Sucursal Norte", "Sucursal Sur");
        }

        @Test
        @DisplayName("distingue una franquicia inexistente de una sin productos")
        void failsOnUnknownFranchise() {
            StepVerifier.create(findTopStock.execute(UUID.randomUUID()))
                    .expectError(NotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("omite las sucursales que aún no ofertan productos")
        void skipsEmptyBranches() {
            Franchise franchise = createFranchise.execute("Vive Fresh").block();
            franchise = addBranch.execute(franchise.id(), "Sucursal Vacía").block();

            StepVerifier.create(findTopStock.execute(franchise.id()))
                    .verifyComplete();
        }
    }

    private Franchise franchiseWithProduct(String productName, long stock) {
        Franchise franchise = createFranchise.execute("Vive Fresh " + UUID.randomUUID()).block();
        franchise = addBranch.execute(franchise.id(), "Sucursal Norte").block();
        UUID branchId = franchise.branches().getFirst().id();
        return addProduct.execute(franchise.id(), branchId, productName, stock).block();
    }
}
