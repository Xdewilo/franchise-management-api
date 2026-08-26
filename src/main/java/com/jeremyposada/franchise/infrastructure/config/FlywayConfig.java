package com.jeremyposada.franchise.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ejecuta las migraciones de esquema durante el arranque.
 *
 * <p><b>Por qué hace falta configuración propia.</b> Flyway sólo habla JDBC y
 * la aplicación es enteramente R2DBC. La salida habitual —añadir el starter de
 * JDBC— arrastraría un pool de conexiones adicional que quedaría abierto toda
 * la vida del proceso para no usarse jamás después del arranque.
 *
 * <p>Aquí Flyway construye su propia conexión, migra y la cierra: no queda
 * ningún pool JDBC residente y el tráfico del API sigue siendo 100% reactivo.
 *
 * <p>La URL JDBC se deriva de la misma propiedad {@code spring.r2dbc.url} para
 * que no exista una segunda copia de los datos de conexión que pueda quedar
 * desincronizada.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    private static final String R2DBC_SCHEME = "r2dbc:";
    private static final String JDBC_SCHEME = "jdbc:";

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    /**
     * Aplica las migraciones pendientes y devuelve la instancia ya ejecutada.
     *
     * <p>Se crea como bean singleton, de modo que la migración termina antes
     * de que el servidor empiece a aceptar peticiones.
     *
     * @return la instancia de Flyway tras migrar
     */
    @Bean
    public Flyway flyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(toJdbcUrl(r2dbcUrl), username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();
        log.info("Esquema en la versión {} ({} migraciones aplicadas)",
                result.targetSchemaVersion, result.migrationsExecuted);
        return flyway;
    }

    /**
     * Traduce una URL R2DBC a su equivalente JDBC.
     *
     * @param url URL de conexión reactiva
     * @return la URL JDBC equivalente
     */
    static String toJdbcUrl(String url) {
        if (!url.startsWith(R2DBC_SCHEME)) {
            throw new IllegalArgumentException("Se esperaba una URL R2DBC y se recibió: " + url);
        }
        return JDBC_SCHEME + url.substring(R2DBC_SCHEME.length());
    }
}
