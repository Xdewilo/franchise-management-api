package com.jeremyposada.franchise.interfaces.rest;

import com.jeremyposada.franchise.infrastructure.persistence.PostgresTestSupport;
import com.jeremyposada.franchise.interfaces.rest.dto.FranchiseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorre el API completo contra un PostgreSQL real, ejercitando los siete
 * criterios de aceptación y los tres puntos extra de renombrado.
 *
 * <p>Es la prueba que verifica el sistema tal como lo verá quien lo evalúe:
 * peticiones HTTP reales, serialización real y persistencia real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("API de franquicias — extremo a extremo")
class FranchiseApiIT extends PostgresTestSupport {

    private static final String FRANCHISES = "/api/v1/franchises";

    @Autowired
    private WebTestClient client;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        databaseClient.sql("TRUNCATE TABLE franchises").fetch().rowsUpdated().block();
    }

    @Nested
    @DisplayName("Criterios de aceptación")
    class AcceptanceCriteria {

        @Test
        @DisplayName("recorre el flujo completo: franquicia, sucursal, producto, stock y borrado")
        void walksThroughEveryCriterion() {
            // Criterio 2 — alta de franquicia
            FranchiseResponse franchise = createFranchise("Vive Fresh");
            assertThat(franchise.id()).isNotNull();
            assertThat(franchise.version()).isZero();

            // Criterio 3 — alta de sucursal
            franchise = addBranch(franchise.id(), "Sucursal Norte");
            UUID branchId = franchise.branches().getFirst().id();
            assertThat(franchise.branches()).hasSize(1);

            // Criterio 4 — alta de producto
            franchise = addProduct(franchise.id(), branchId, "Arepa de huevo", 25);
            UUID productId = franchise.branches().getFirst().products().getFirst().id();

            // Criterio 6 — modificación de stock
            franchise = client.patch()
                    .uri("%s/%s/branches/%s/products/%s/stock".formatted(
                            FRANCHISES, franchise.id(), branchId, productId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("stock", 40))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(FranchiseResponse.class)
                    .returnResult().getResponseBody();

            assertThat(franchise.branches().getFirst().products().getFirst().stock()).isEqualTo(40L);

            // Criterio 5 — baja de producto
            franchise = client.delete()
                    .uri("%s/%s/branches/%s/products/%s".formatted(
                            FRANCHISES, franchise.id(), branchId, productId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(FranchiseResponse.class)
                    .returnResult().getResponseBody();

            assertThat(franchise.branches().getFirst().products()).isEmpty();
        }

        @Test
        @DisplayName("criterio 7: devuelve el producto de mayor stock de cada sucursal con su sucursal")
        void returnsTopStockProductPerBranch() {
            FranchiseResponse franchise = createFranchise("Vive Fresh");
            franchise = addBranch(franchise.id(), "Sucursal Norte");
            franchise = addBranch(franchise.id(), "Sucursal Sur");
            UUID north = franchise.branches().getFirst().id();
            UUID south = franchise.branches().getLast().id();

            addProduct(franchise.id(), north, "Arepa de huevo", 25);
            addProduct(franchise.id(), north, "Jugo de mango", 80);
            addProduct(franchise.id(), south, "Café", 10);

            client.get()
                    .uri("%s/%s/branches/top-stock-products".formatted(FRANCHISES, franchise.id()))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[0].branchName").isEqualTo("Sucursal Norte")
                    .jsonPath("$[0].productName").isEqualTo("Jugo de mango")
                    .jsonPath("$[0].stock").isEqualTo(80)
                    .jsonPath("$[1].branchName").isEqualTo("Sucursal Sur")
                    .jsonPath("$[1].productName").isEqualTo("Café");
        }
    }

    @Nested
    @DisplayName("Puntos extra de renombrado")
    class RenameOperations {

        @Test
        @DisplayName("renombra franquicia, sucursal y producto")
        void renamesEveryLevel() {
            FranchiseResponse franchise = createFranchise("Vive Fresh");
            franchise = addBranch(franchise.id(), "Sucursal Norte");
            UUID branchId = franchise.branches().getFirst().id();
            franchise = addProduct(franchise.id(), branchId, "Arepa de huevo", 25);
            UUID productId = franchise.branches().getFirst().products().getFirst().id();

            rename("%s/%s/name".formatted(FRANCHISES, franchise.id()), "Vive Fresh Premium")
                    .jsonPath("$.name").isEqualTo("Vive Fresh Premium");

            rename("%s/%s/branches/%s/name".formatted(FRANCHISES, franchise.id(), branchId), "Sucursal Centro")
                    .jsonPath("$.branches[0].name").isEqualTo("Sucursal Centro");

            rename("%s/%s/branches/%s/products/%s/name".formatted(
                    FRANCHISES, franchise.id(), branchId, productId), "Arepa de queso")
                    .jsonPath("$.branches[0].products[0].name").isEqualTo("Arepa de queso");
        }
    }

    @Nested
    @DisplayName("Manejo de errores")
    class ErrorHandling {

        @Test
        @DisplayName("400 con el detalle del campo cuando el cuerpo es inválido")
        void rejectsInvalidBody() {
            client.post().uri(FRANCHISES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("name", "  "))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                    .jsonPath("$.details[0].field").isEqualTo("name");
        }

        @Test
        @DisplayName("400 cuando el stock es negativo")
        void rejectsNegativeStock() {
            FranchiseResponse franchise = createFranchise("Vive Fresh");
            franchise = addBranch(franchise.id(), "Sucursal Norte");
            UUID branchId = franchise.branches().getFirst().id();

            client.post()
                    .uri("%s/%s/branches/%s/products".formatted(FRANCHISES, franchise.id(), branchId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("name", "Arepa", "stock", -5))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details[0].field").isEqualTo("stock");
        }

        @Test
        @DisplayName("404 cuando la franquicia no existe")
        void reportsUnknownFranchise() {
            client.get().uri("%s/%s".formatted(FRANCHISES, UUID.randomUUID()))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
        }

        @Test
        @DisplayName("409 cuando el nombre de franquicia ya está tomado")
        void reportsDuplicateFranchise() {
            createFranchise("Vive Fresh");

            client.post().uri(FRANCHISES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("name", "vive fresh"))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("DUPLICATE_FRANCHISE_NAME");
        }

        @Test
        @DisplayName("409 cuando la sucursal ya existe en la franquicia")
        void reportsDuplicateBranch() {
            FranchiseResponse franchise = createFranchise("Vive Fresh");
            addBranch(franchise.id(), "Sucursal Norte");

            client.post().uri("%s/%s/branches".formatted(FRANCHISES, franchise.id()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("name", "SUCURSAL NORTE"))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("DUPLICATE_BRANCH_NAME");
        }

        @Test
        @DisplayName("400 cuando el identificador de la ruta no es un UUID")
        void rejectsMalformedIdentifier() {
            client.get().uri(FRANCHISES + "/no-es-un-uuid")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST");
        }
    }

    @Nested
    @DisplayName("Punto de entrada")
    class Entrypoint {

        @Test
        @DisplayName("la raíz redirige a la documentación interactiva")
        void rootRedirectsToDocumentation() {
            client.get().uri("/")
                    .exchange()
                    .expectStatus().isFound()
                    .expectHeader().location("/swagger-ui.html");
        }
    }

    // --- Utilidades ---------------------------------------------------------

    private FranchiseResponse createFranchise(String name) {
        return client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult().getResponseBody();
    }

    private FranchiseResponse addBranch(UUID franchiseId, String name) {
        return client.post().uri("%s/%s/branches".formatted(FRANCHISES, franchiseId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult().getResponseBody();
    }

    private FranchiseResponse addProduct(UUID franchiseId, UUID branchId, String name, long stock) {
        return client.post().uri("%s/%s/branches/%s/products".formatted(FRANCHISES, franchiseId, branchId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name, "stock", stock))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult().getResponseBody();
    }

    private WebTestClient.BodyContentSpec rename(String uri, String newName) {
        return client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", newName))
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }
}
