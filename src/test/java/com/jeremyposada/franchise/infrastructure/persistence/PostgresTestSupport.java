package com.jeremyposada.franchise.infrastructure.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests que necesitan un PostgreSQL real.
 *
 * <p>Se usa una instancia efímera en contenedor —no una base embebida ni H2—
 * porque el adaptador se apoya en características propias de PostgreSQL
 * (JSONB, {@code LATERAL}, {@code DISTINCT ON}) que ningún sustituto reproduce.
 * Probar contra otro motor daría una confianza que no se corresponde con lo
 * que corre en producción.
 *
 * <p>El contenedor se declara {@code static}: se levanta una vez para toda la
 * clase en lugar de por cada test.
 */
@Testcontainers
public abstract class PostgresTestSupport {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("franchises")
            .withUsername("franchise")
            .withPassword("franchise");

    /**
     * Apunta la aplicación al contenedor. Se fija {@code spring.r2dbc.url}
     * explícitamente porque de ahí deriva también la URL JDBC que usa Flyway.
     */
    @DynamicPropertySource
    static void registerConnection(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }
}
