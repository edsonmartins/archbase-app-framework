package br.com.archbase.ddd.infraestructure.service;


import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.query.rsql.jpa.SortUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;


public abstract class SimpleArchbaseQueryService<T, ID extends Serializable, N extends Number & Comparable<N>> {

    private Repository<T, ID, N> repository;

    @Autowired
    protected SimpleArchbaseQueryService(Repository<T, ID, N> repository) {
        this.repository = repository;
    }

    public Repository<T, ID, N> getRepository() {
        return repository;
    }

    /**
     * Busca um objeto pelo seu ID.
     *
     * @param id Identificador do objeto.
     * @return Objeto encontrado.
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public T findOne(@PathVariable(value = "id") String id) {
        ID castID = (ID) id;
        Optional<T> result = getRepository().findById(castID);
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }

    /**
     * Busca um objeto pelo seu ID complexo.
     *
     * @param id Identificador do objeto.
     * @return Objeto encontrado.
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public T findByComplexId(ID id) {
        Optional<T> result = getRepository().findById(id);
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }

    /**
     * Busca os objetos da classe com paginação.
     *
     * @param page Número da página
     * @param size Tamanho da página
     * @return Página
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return getRepository().findAll(pageable);
    }

    /**
     * Busca os objetos da classe com paginação e ordenado
     *
     * @param page Número da página
     * @param size Tamanho da página
     * @param sort Campos para ordenação
     * @return Página
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return getRepository().findAll(pageable);
    }

    /**
     * Busca os objetos da classe contido na lista de ID's.
     *
     * @param ids Lista de ID's
     * @return Lista de objetos encontrados.
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public List<T> findAll(List<ID> ids) {
        return getRepository().findAllById(ids);
    }

    /**
     * Busca os objetos da classe de acordo com o objeto filtro.
     *
     * @param filter String RSQL
     * @param page   Número da página
     * @param size   Tamanho da página
     * @return Página
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> find(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return getRepository().findAll(filter, pageable);
    }

    /**
     * Busca os objetos da classe de acordo com o objeto filtro.
     *
     * @param filter String RSQL
     * @param page   Número da página
     * @param size   Tamanho da página
     * @param sort   Ordenação
     * @return Página
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> find(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return getRepository().findAll(filter, pageable);
    }

    /**
     * Count
     */

    /**
     * Retorna a quantidade de objetos da classe.
     *
     * @return Número total de objetos
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public long count() {
        return getRepository().count();
    }

    /**
     * Verifica a existência de um objeto com o ID.
     *
     * @param id Id do objeto
     * @return Verdadeiro se existir.
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public boolean exists(@PathVariable String id) {
        ID castID = (ID) id;
        return getRepository().existsById(castID);
    }

    /**
     * Verifica a existência de um objeto pelo seu id complexo.
     *
     * @param id Id do objeto
     * @return Verdadeiro se existir algum id.
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public boolean existsByComplexId(ID id) {
        return getRepository().existsById(id);
    }


}
