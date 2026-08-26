package com.jeremyposada.franchise.interfaces.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Envía la raíz del servicio a la documentación interactiva.
 *
 * <p>Sin esto, quien abra la URL del despliegue sin ninguna ruta se encuentra
 * un 404 y tiene que adivinar por dónde empezar. Redirigir a Swagger UI
 * convierte la raíz en el punto de entrada natural: desde ahí se ven y se
 * ejecutan todos los endpoints.
 *
 * <p>Se marca {@code @Hidden} para que la propia redirección no aparezca como
 * una operación más del API.
 */
@RestController
@Hidden
public class RootRedirectController {

    private static final URI DOCUMENTATION = URI.create("/swagger-ui.html");

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND).location(DOCUMENTATION).build();
    }
}
