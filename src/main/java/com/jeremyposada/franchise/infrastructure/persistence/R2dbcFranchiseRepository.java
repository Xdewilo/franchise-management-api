package com.jeremyposada.franchise.infrastructure.persistence;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.model.Franchise;
import com.jeremyposada.franchise.domain.model.Name;
import com.jeremyposada.franchise.domain.model.TopStockProduct;
import com.jeremyposada.franchise.domain.port.FranchiseRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador de salida sobre PostgreSQL mediante R2DBC.
 *
 * <p><b>Por qué SQL explícito y no un repositorio derivado.</b> El agregado se
 * guarda completo en una columna JSONB y su identidad se genera en la
 * aplicación, no en la base de datos. Con un {@code ReactiveCrudRepository}
 * eso obliga a pelear con la heurística que decide si una entidad es nueva
 * —una fila con id asignado se interpreta como existente y se intenta
 * actualizar— y deja el bloqueo optimista en manos de esa misma heurística.
 * Escribiendo el SQL, tanto la inserción como el UPDATE condicionado por
 * versión quedan a la vista y bajo control.
 *
 * <p>El árbol se castea a {@code jsonb} en la propia consulta y se lee como
 * texto. Así el adaptador sólo depende de la API estándar {@code io.r2dbc.spi}
 * y el driver de PostgreSQL puede seguir siendo una dependencia de ejecución,
 * intercambiable sin tocar este código.
 */
@Repository
@RequiredArgsConstructor
public class R2dbcFranchiseRepository implements FranchiseRepository {

    private static final String INSERT = """
            INSERT INTO franchises (id, name, branches, version)
            VALUES (:id, :name, CAST(:branches AS jsonb), 0)
            """;

    /**
     * El UPDATE lleva la versión esperada en el WHERE: si otra petición ya
     * escribió, ninguna fila coincide y la operación se reporta como
     * conflicto en lugar de sobrescribir en silencio.
     */
    private static final String UPDATE = """
            UPDATE franchises
               SET name       = :name,
                   branches   = CAST(:branches AS jsonb),
                   version    = version + 1,
                   updated_at = now()
             WHERE id = :id
               AND version = :expectedVersion
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, branches::text AS branches, version
              FROM franchises
             WHERE id = :id
            """;

    private static final String SELECT_PAGE = """
            SELECT id, name, branches::text AS branches, version
              FROM franchises
             ORDER BY name
             LIMIT :size OFFSET :offset
            """;

    private static final String COUNT = "SELECT count(*) AS total FROM franchises";

    private static final String EXISTS_BY_NAME = """
            SELECT EXISTS (
                SELECT 1 FROM franchises WHERE lower(name) = lower(:name)
            ) AS taken
            """;

    private static final String EXISTS_BY_NAME_EXCLUDING = """
            SELECT EXISTS (
                SELECT 1 FROM franchises WHERE lower(name) = lower(:name) AND id <> :excluding
            ) AS taken
            """;

    /**
     * Criterio 7 resuelto dentro del motor.
     *
     * <p>Los dos {@code LATERAL} despliegan el árbol JSONB en filas
     * —una por producto— y {@code DISTINCT ON (sucursal)} se queda con la
     * primera de cada grupo según el orden indicado, que es el producto de
     * mayor stock. El desempate por nombre hace la respuesta determinista
     * cuando dos productos empatan en existencias.
     *
     * <p>La alternativa —traer el agregado completo y recorrerlo en Java—
     * funciona igual con datos de prueba, pero obliga a mover todo el catálogo
     * a la JVM para devolver una fila por sucursal.
     */
    private static final String TOP_STOCK_PER_BRANCH = """
            SELECT *
              FROM (
                    SELECT DISTINCT ON (branch.value ->> 'id')
                           (branch.value  ->> 'id')::uuid       AS branch_id,
                            branch.value  ->> 'name'            AS branch_name,
                           (product.value ->> 'id')::uuid       AS product_id,
                            product.value ->> 'name'            AS product_name,
                           (product.value ->> 'stock')::bigint  AS stock
                      FROM franchises f
                      CROSS JOIN LATERAL jsonb_array_elements(f.branches)              AS branch(value)
                      CROSS JOIN LATERAL jsonb_array_elements(branch.value->'products') AS product(value)
                     WHERE f.id = :franchiseId
                     ORDER BY branch.value ->> 'id',
                              (product.value ->> 'stock')::bigint DESC,
                              product.value ->> 'name' ASC
                   ) top_per_branch
             ORDER BY branch_name
            """;

    private final DatabaseClient databaseClient;
    private final FranchiseDocumentMapper mapper;

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return franchise.version() == null ? insert(franchise) : update(franchise);
    }

    private Mono<Franchise> insert(Franchise franchise) {
        return databaseClient.sql(INSERT)
                .bind("id", franchise.id())
                .bind("name", franchise.name().value())
                .bind("branches", mapper.toJson(franchise))
                .fetch()
                .rowsUpdated()
                .thenReturn(withVersion(franchise, 0L))
                .onErrorMap(DataIntegrityViolationException.class,
                        cause -> ConflictException.duplicateFranchiseName(franchise.name().value()));
    }

    private Mono<Franchise> update(Franchise franchise) {
        long expectedVersion = franchise.version();
        return databaseClient.sql(UPDATE)
                .bind("id", franchise.id())
                .bind("name", franchise.name().value())
                .bind("branches", mapper.toJson(franchise))
                .bind("expectedVersion", expectedVersion)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 0
                        ? Mono.<Franchise>error(ConflictException.concurrentModification())
                        : Mono.just(withVersion(franchise, expectedVersion + 1)))
                .onErrorMap(DataIntegrityViolationException.class,
                        cause -> ConflictException.duplicateFranchiseName(franchise.name().value()));
    }

    @Override
    public Mono<Franchise> findById(UUID franchiseId) {
        return databaseClient.sql(SELECT_BY_ID)
                .bind("id", franchiseId)
                .map(this::toFranchise)
                .one();
    }

    @Override
    public Mono<Boolean> existsByName(Name name, UUID excluding) {
        DatabaseClient.GenericExecuteSpec query = excluding == null
                ? databaseClient.sql(EXISTS_BY_NAME)
                : databaseClient.sql(EXISTS_BY_NAME_EXCLUDING).bind("excluding", excluding);

        return query.bind("name", name.value())
                .map((row, metadata) -> row.get("taken", Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<Franchise> findAll(int page, int size) {
        return databaseClient.sql(SELECT_PAGE)
                .bind("size", size)
                .bind("offset", (long) page * size)
                .map(this::toFranchise)
                .all();
    }

    @Override
    public Mono<Long> count() {
        return databaseClient.sql(COUNT)
                .map((row, metadata) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Flux<TopStockProduct> findTopStockProductPerBranch(UUID franchiseId) {
        return databaseClient.sql(TOP_STOCK_PER_BRANCH)
                .bind("franchiseId", franchiseId)
                .map((row, metadata) -> new TopStockProduct(
                        row.get("branch_id", UUID.class),
                        row.get("branch_name", String.class),
                        row.get("product_id", UUID.class),
                        row.get("product_name", String.class),
                        row.get("stock", Long.class)))
                .all();
    }

    private Franchise toFranchise(Row row, RowMetadata metadata) {
        String branches = row.get("branches", String.class);
        return mapper.toDomain(
                row.get("id", UUID.class),
                row.get("name", String.class),
                branches == null ? "[]" : branches,
                row.get("version", Long.class));
    }

    private static Franchise withVersion(Franchise franchise, long version) {
        return new Franchise(franchise.id(), franchise.name(), franchise.branches(), version);
    }
}
