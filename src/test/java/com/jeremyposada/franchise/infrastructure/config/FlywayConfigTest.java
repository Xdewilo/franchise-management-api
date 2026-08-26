package com.jeremyposada.franchise.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FlywayConfig — derivación de la URL JDBC")
class FlywayConfigTest {

    @Test
    @DisplayName("cambia el esquema de una URL local")
    void convertsPlainUrl() {
        assertThat(FlywayConfig.toJdbcUrl("r2dbc:postgresql://localhost:5432/franchises"))
                .isEqualTo("jdbc:postgresql://localhost:5432/franchises");
    }

    @Test
    @DisplayName("traduce el parámetro de TLS al nombre que espera JDBC")
    void translatesSslParameter() {
        String neonUrl = "r2dbc:postgresql://ep-green-dawn.us-east-2.aws.neon.tech/franchises?sslMode=require";

        assertThat(FlywayConfig.toJdbcUrl(neonUrl))
                .isEqualTo("jdbc:postgresql://ep-green-dawn.us-east-2.aws.neon.tech/franchises?sslmode=require");
    }

    @Test
    @DisplayName("conserva el resto de parámetros de la cadena")
    void keepsOtherParameters() {
        String url = "r2dbc:postgresql://host/db?sslMode=require&connectTimeout=10s";

        assertThat(FlywayConfig.toJdbcUrl(url))
                .isEqualTo("jdbc:postgresql://host/db?sslmode=require&connectTimeout=10s");
    }

    @Test
    @DisplayName("rechaza una URL que no sea R2DBC")
    void rejectsNonR2dbcUrl() {
        assertThatThrownBy(() -> FlywayConfig.toJdbcUrl("jdbc:postgresql://localhost/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("R2DBC");
    }
}
