package com.jeremyposada.franchise.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación viva del API, servida en {@code /swagger-ui.html}.
 *
 * <p>Se genera desde el propio código, de modo que no puede quedar
 * desactualizada respecto a los endpoints que realmente existen.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Franchise Management API")
                .version("1.0.0")
                .description("""
                        API reactiva para la gestión de franquicias, sucursales y productos.

                        El modelo tiene un único aggregate root, Franquicia, que contiene sus \
                        sucursales y los productos de cada una. Por eso las operaciones sobre \
                        sucursales y productos cuelgan de la ruta de su franquicia y devuelven \
                        el árbol completo ya actualizado.
                        """)
                .contact(new Contact().name("Jeremy Posada").url("https://github.com/Xdewilo"))
                .license(new License().name("MIT")));
    }
}
