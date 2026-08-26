package com.jeremyposada.franchise.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeremyposada.franchise.domain.model.Branch;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.Product;
import com.jeremyposada.franchise.domain.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Traduce el árbol de sucursales entre el modelo de dominio y el JSON que se
 * almacena en la columna {@code branches}.
 *
 * <p>La conversión es explícita a propósito. Serializar los value objects
 * directamente produciría un JSON con su estructura interna
 * ({@code {"name": {"value": "..."}}}), atando el formato de los datos
 * persistidos a decisiones internas del dominio.
 */
@Component
@RequiredArgsConstructor
public class FranchiseDocumentMapper {

    private static final TypeReference<List<BranchDocument>> BRANCH_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * Serializa las sucursales de un agregado.
     *
     * @param franchise agregado de origen
     * @return el JSON a almacenar en la columna {@code branches}
     */
    public String toJson(Franchise franchise) {
        List<BranchDocument> documents = franchise.branches().stream()
                .map(FranchiseDocumentMapper::toDocument)
                .toList();
        try {
            return objectMapper.writeValueAsString(documents);
        } catch (JsonProcessingException cause) {
            throw new IllegalStateException(
                    "No se pudo serializar el árbol de la franquicia " + franchise.id(), cause);
        }
    }

    /**
     * Reconstruye el agregado a partir de una fila de la tabla.
     *
     * @param id       identidad de la franquicia
     * @param name     nombre almacenado
     * @param json     contenido de la columna {@code branches}
     * @param version  versión almacenada
     * @return el agregado de dominio
     */
    public Franchise toDomain(UUID id, String name, String json, Long version) {
        try {
            List<Branch> branches = objectMapper.readValue(json, BRANCH_LIST).stream()
                    .map(FranchiseDocumentMapper::toDomain)
                    .toList();
            return new Franchise(id, new Name(name), branches, version);
        } catch (JsonProcessingException cause) {
            throw new IllegalStateException(
                    "El árbol almacenado de la franquicia " + id + " no es JSON válido", cause);
        }
    }

    private static BranchDocument toDocument(Branch branch) {
        return new BranchDocument(
                branch.id(),
                branch.name().value(),
                branch.products().stream()
                        .map(product -> new ProductDocument(
                                product.id(), product.name().value(), product.stock().value()))
                        .toList());
    }

    private static Branch toDomain(BranchDocument document) {
        return new Branch(
                document.id(),
                new Name(document.name()),
                document.products().stream()
                        .map(product -> new Product(
                                product.id(), new Name(product.name()), new Stock(product.stock())))
                        .toList());
    }
}
