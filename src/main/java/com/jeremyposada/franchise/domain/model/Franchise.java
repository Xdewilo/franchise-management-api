package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.ConflictException;
import com.jeremyposada.franchise.domain.exception.NotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Aggregate root del modelo: una franquicia con todas sus sucursales y los
 * productos que éstas ofertan.
 *
 * <p><b>Por qué es la única raíz.</b> Sucursales y productos no tienen sentido
 * ni ciclo de vida fuera de su franquicia, y todas las reglas de unicidad
 * ("no dos sucursales con el mismo nombre", "no dos productos iguales en una
 * sucursal") se evalúan sobre el árbol completo. Al hacer coincidir la
 * frontera del agregado con la frontera transaccional, cada operación es una
 * sola escritura atómica: no hay estado a medias.
 *
 * <p><b>Inmutabilidad.</b> Toda operación devuelve una instancia nueva en vez
 * de mutar la actual. El agregado no tiene setters ni estado compartido, así
 * que es seguro entre hilos —relevante en un stack reactivo, donde la
 * ejecución salta de hilo— y cada test parte de un estado que nadie pudo
 * alterar por el camino.
 *
 * <p>La clase no conoce Spring, R2DBC ni HTTP: es Java puro y se testea sin
 * levantar contexto alguno.
 *
 * @param id       identidad de la franquicia
 * @param name     nombre comercial, único en todo el sistema
 * @param branches sucursales; lista inmutable
 * @param version  versión del agregado para bloqueo optimista;
 *                 {@code null} mientras no se haya persistido
 */
public record Franchise(UUID id, Name name, List<Branch> branches, Long version) {

    public Franchise {
        branches = List.copyOf(branches);
    }

    /**
     * Crea una franquicia nueva, sin sucursales y aún sin versión.
     *
     * @param name nombre comercial
     * @return la franquicia creada
     */
    public static Franchise create(Name name) {
        return new Franchise(UUID.randomUUID(), name, List.of(), null);
    }

    /**
     * Renombra la franquicia.
     *
     * @param newName nombre nuevo
     * @return copia con el nombre actualizado
     */
    public Franchise rename(Name newName) {
        return new Franchise(id, newName, branches, version);
    }

    /**
     * Abre una sucursal.
     *
     * @param branchName nombre de la sucursal
     * @return copia con la sucursal añadida
     * @throws ConflictException si ya existe una sucursal con ese nombre
     */
    public Franchise addBranch(Name branchName) {
        if (hasBranchNamed(branchName, null)) {
            throw ConflictException.duplicateBranchName(branchName.value());
        }
        return withBranches(append(branches, Branch.create(branchName)));
    }

    /**
     * Renombra una sucursal.
     *
     * @param branchId identidad de la sucursal
     * @param newName  nombre nuevo
     * @return copia con la sucursal renombrada
     * @throws NotFoundException si la sucursal no pertenece a esta franquicia
     * @throws ConflictException si otra sucursal ya usa ese nombre
     */
    public Franchise renameBranch(UUID branchId, Name newName) {
        requireBranch(branchId);
        if (hasBranchNamed(newName, branchId)) {
            throw ConflictException.duplicateBranchName(newName.value());
        }
        return updateBranch(branchId, branch -> branch.withName(newName));
    }

    /**
     * Añade un producto al catálogo de una sucursal.
     *
     * @param branchId    identidad de la sucursal
     * @param productName nombre del producto
     * @param stock       existencias iniciales
     * @return copia con el producto añadido
     * @throws NotFoundException si la sucursal no pertenece a esta franquicia
     * @throws ConflictException si la sucursal ya oferta ese producto
     */
    public Franchise addProduct(UUID branchId, Name productName, Stock stock) {
        return updateBranch(branchId, branch -> branch.addProduct(productName, stock));
    }

    /**
     * Retira un producto del catálogo de una sucursal.
     *
     * @param branchId  identidad de la sucursal
     * @param productId identidad del producto
     * @return copia sin ese producto
     * @throws NotFoundException si la sucursal o el producto no existen
     */
    public Franchise removeProduct(UUID branchId, UUID productId) {
        return updateBranch(branchId, branch -> branch.removeProduct(productId));
    }

    /**
     * Renombra un producto.
     *
     * @param branchId  identidad de la sucursal
     * @param productId identidad del producto
     * @param newName   nombre nuevo
     * @return copia con el producto renombrado
     * @throws NotFoundException si la sucursal o el producto no existen
     * @throws ConflictException si la sucursal ya oferta otro producto con ese nombre
     */
    public Franchise renameProduct(UUID branchId, UUID productId, Name newName) {
        return updateBranch(branchId, branch -> branch.renameProduct(productId, newName));
    }

    /**
     * Ajusta las existencias de un producto.
     *
     * @param branchId  identidad de la sucursal
     * @param productId identidad del producto
     * @param newStock  existencias nuevas
     * @return copia con el stock actualizado
     * @throws NotFoundException si la sucursal o el producto no existen
     */
    public Franchise updateProductStock(UUID branchId, UUID productId, Stock newStock) {
        return updateBranch(branchId, branch -> branch.updateProductStock(productId, newStock));
    }

    /**
     * Busca una sucursal por identidad.
     *
     * @param branchId identidad de la sucursal
     * @return la sucursal
     * @throws NotFoundException si no pertenece a esta franquicia
     */
    public Branch requireBranch(UUID branchId) {
        return branches.stream()
                .filter(branch -> branch.id().equals(branchId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.branch(branchId));
    }

    private Franchise updateBranch(UUID branchId, UnaryOperator<Branch> change) {
        requireBranch(branchId);
        return withBranches(branches.stream()
                .map(branch -> branch.id().equals(branchId) ? change.apply(branch) : branch)
                .toList());
    }

    private Franchise withBranches(List<Branch> newBranches) {
        return new Franchise(id, name, newBranches, version);
    }

    /**
     * @param candidate nombre a verificar
     * @param excluding sucursal excluida de la comparación (la que se está
     *                  renombrando), o {@code null} para no excluir ninguna
     */
    private boolean hasBranchNamed(Name candidate, UUID excluding) {
        return branches.stream()
                .filter(branch -> !branch.id().equals(excluding))
                .anyMatch(branch -> branch.name().matches(candidate));
    }

    private static <T> List<T> append(List<T> source, T element) {
        return Stream.concat(source.stream(), Stream.of(element)).toList();
    }
}
