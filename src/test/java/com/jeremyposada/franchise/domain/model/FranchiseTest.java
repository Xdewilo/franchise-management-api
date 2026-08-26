package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Franchise — aggregate root")
class FranchiseTest {

    private static final Name FRANCHISE_NAME = new Name("Vive Fresh");
    private static final Name BRANCH_NAME = new Name("Sucursal Norte");
    private static final Name PRODUCT_NAME = new Name("Arepa de huevo");

    @Nested
    @DisplayName("Creación")
    class Creation {

        @Test
        @DisplayName("nace con identidad propia, sin sucursales y sin versión")
        void startsEmpty() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME);

            assertThat(franchise.id()).isNotNull();
            assertThat(franchise.name()).isEqualTo(FRANCHISE_NAME);
            assertThat(franchise.branches()).isEmpty();
            assertThat(franchise.version()).isNull();
        }
    }

    @Nested
    @DisplayName("Sucursales")
    class Branches {

        @Test
        @DisplayName("agrega una sucursal conservando la identidad de la franquicia")
        void addsBranch() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME).addBranch(BRANCH_NAME);

            assertThat(franchise.branches()).hasSize(1);
            assertThat(franchise.branches().getFirst().name()).isEqualTo(BRANCH_NAME);
            assertThat(franchise.branches().getFirst().products()).isEmpty();
        }

        @Test
        @DisplayName("rechaza dos sucursales con el mismo nombre aunque cambie el uso de mayúsculas")
        void rejectsDuplicateBranchName() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME).addBranch(BRANCH_NAME);

            assertThatThrownBy(() -> franchise.addBranch(new Name("  sucursal norte ")))
                    .isInstanceOf(ConflictException.class)
                    .extracting("code")
                    .isEqualTo(DomainErrorCode.DUPLICATE_BRANCH_NAME);
        }

        @Test
        @DisplayName("renombra una sucursal existente")
        void renamesBranch() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME).addBranch(BRANCH_NAME);
            UUID branchId = franchise.branches().getFirst().id();

            Franchise renamed = franchise.renameBranch(branchId, new Name("Sucursal Centro"));

            assertThat(renamed.requireBranch(branchId).name().value()).isEqualTo("Sucursal Centro");
        }

        @Test
        @DisplayName("permite renombrar una sucursal a su mismo nombre")
        void allowsRenamingBranchToItsOwnName() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME).addBranch(BRANCH_NAME);
            UUID branchId = franchise.branches().getFirst().id();

            Franchise renamed = franchise.renameBranch(branchId, BRANCH_NAME);

            assertThat(renamed.requireBranch(branchId).name()).isEqualTo(BRANCH_NAME);
        }

        @Test
        @DisplayName("rechaza renombrar una sucursal con el nombre de otra")
        void rejectsRenamingBranchToAnExistingName() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME)
                    .addBranch(BRANCH_NAME)
                    .addBranch(new Name("Sucursal Sur"));
            UUID branchId = franchise.branches().getFirst().id();

            assertThatThrownBy(() -> franchise.renameBranch(branchId, new Name("Sucursal Sur")))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("falla al operar sobre una sucursal inexistente")
        void failsOnUnknownBranch() {
            Franchise franchise = Franchise.create(FRANCHISE_NAME);

            assertThatThrownBy(() -> franchise.requireBranch(UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("code")
                    .isEqualTo(DomainErrorCode.BRANCH_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Productos")
    class Products {

        @Test
        @DisplayName("agrega un producto a la sucursal indicada")
        void addsProduct() {
            Franchise franchise = franchiseWithBranch();
            UUID branchId = franchise.branches().getFirst().id();

            Franchise updated = franchise.addProduct(branchId, PRODUCT_NAME, new Stock(25));

            Product product = updated.requireBranch(branchId).products().getFirst();
            assertThat(product.name()).isEqualTo(PRODUCT_NAME);
            assertThat(product.stock().value()).isEqualTo(25L);
        }

        @Test
        @DisplayName("rechaza dos productos con el mismo nombre en la misma sucursal")
        void rejectsDuplicateProductInSameBranch() {
            Franchise franchise = franchiseWithProduct();
            UUID branchId = franchise.branches().getFirst().id();

            assertThatThrownBy(() -> franchise.addProduct(branchId, PRODUCT_NAME, new Stock(1)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("code")
                    .isEqualTo(DomainErrorCode.DUPLICATE_PRODUCT_NAME);
        }

        @Test
        @DisplayName("admite el mismo producto en sucursales distintas")
        void allowsSameProductInDifferentBranches() {
            Franchise franchise = franchiseWithProduct().addBranch(new Name("Sucursal Sur"));
            UUID otherBranchId = franchise.branches().getLast().id();

            Franchise updated = franchise.addProduct(otherBranchId, PRODUCT_NAME, new Stock(3));

            assertThat(updated.requireBranch(otherBranchId).products()).hasSize(1);
        }

        @Test
        @DisplayName("elimina un producto de la sucursal")
        void removesProduct() {
            Franchise franchise = franchiseWithProduct();
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            Franchise updated = franchise.removeProduct(branchId, productId);

            assertThat(updated.requireBranch(branchId).products()).isEmpty();
        }

        @Test
        @DisplayName("falla al eliminar un producto que no pertenece a la sucursal")
        void failsRemovingUnknownProduct() {
            Franchise franchise = franchiseWithBranch();
            UUID branchId = franchise.branches().getFirst().id();

            assertThatThrownBy(() -> franchise.removeProduct(branchId, UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("code")
                    .isEqualTo(DomainErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("actualiza el stock de un producto")
        void updatesStock() {
            Franchise franchise = franchiseWithProduct();
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            Franchise updated = franchise.updateProductStock(branchId, productId, new Stock(99));

            assertThat(updated.requireBranch(branchId).requireProduct(productId).stock().value()).isEqualTo(99L);
        }

        @Test
        @DisplayName("renombra un producto")
        void renamesProduct() {
            Franchise franchise = franchiseWithProduct();
            UUID branchId = franchise.branches().getFirst().id();
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            Franchise updated = franchise.renameProduct(branchId, productId, new Name("Arepa de queso"));

            assertThat(updated.requireBranch(branchId).requireProduct(productId).name().value())
                    .isEqualTo("Arepa de queso");
        }

        @Test
        @DisplayName("rechaza renombrar un producto con el nombre de otro de la misma sucursal")
        void rejectsRenamingProductToAnExistingName() {
            Franchise franchise = franchiseWithProduct();
            UUID branchId = franchise.branches().getFirst().id();
            franchise = franchise.addProduct(branchId, new Name("Jugo de mango"), new Stock(4));
            UUID productId = franchise.requireBranch(branchId).products().getFirst().id();

            Franchise target = franchise;
            assertThatThrownBy(() -> target.renameProduct(branchId, productId, new Name("Jugo de mango")))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class Immutability {

        @Test
        @DisplayName("cada operación devuelve una instancia nueva y deja intacta la anterior")
        void operationsDoNotMutateTheOriginal() {
            Franchise original = Franchise.create(FRANCHISE_NAME);

            Franchise modified = original.addBranch(BRANCH_NAME);

            assertThat(original.branches()).isEmpty();
            assertThat(modified.branches()).hasSize(1);
            assertThat(modified).isNotSameAs(original);
        }

        @Test
        @DisplayName("expone la lista de sucursales como copia inmutable")
        void exposesUnmodifiableBranches() {
            Franchise franchise = franchiseWithBranch();

            assertThatThrownBy(() -> franchise.branches().add(Branch.create(new Name("Pirata"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("conserva la versión del agregado tras modificarlo")
        void keepsVersionAcrossChanges() {
            Franchise persisted = new Franchise(UUID.randomUUID(), FRANCHISE_NAME, java.util.List.of(), 7L);

            assertThat(persisted.addBranch(BRANCH_NAME).version()).isEqualTo(7L);
        }
    }

    private static Franchise franchiseWithBranch() {
        return Franchise.create(FRANCHISE_NAME).addBranch(BRANCH_NAME);
    }

    private static Franchise franchiseWithProduct() {
        Franchise franchise = franchiseWithBranch();
        return franchise.addProduct(franchise.branches().getFirst().id(), PRODUCT_NAME, new Stock(25));
    }
}
