package br.com.archbase.ddd.infraestructure.resource;

import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.ddd.infraestructure.exceptions.ArchbaseServiceException;
import br.com.archbase.ddd.infraestructure.service.CommonArchbaseService;
import br.com.archbase.query.rsql.jpa.SortUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Validator;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import static br.com.archbase.ddd.infraestructure.constants.DddConstantsErrorCodes.CONTROLLER_ENTIDADE_NOT_FOUND;

@SuppressWarnings("all")
public abstract class CommonArchbaseCommandRestController<T, ID extends Serializable, N extends Number & Comparable<N>> {


    @Autowired
    private MessageSource messageSource;
    @Autowired
    @Qualifier("validator")
    private Validator validator;

    /**
     * Método abstrato que irá fornecer a classe de serviço para ser usada no
     * controller.
     *
     * @return
     */
    public abstract CommonArchbaseService<T, ID, N> getService();

    /**
     * Método abstrato que irá fornecer a classe de repositório para ser usada no
     * controller.
     *
     * @return
     */
    public abstract Repository<T, ID, N> getRepository();

    /**
     * Insere ou atualiza um objeto.
     *
     * @param object Objeto a ser salvo
     * @return Objeto salvo
     * @throws Exception
     */
    @RequestMapping(value = "/", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public T save(@RequestBody T object) {
        validator.validate(object);
        return getRepository().save(object);
    }

    /**
     * Valida um objeto.
     *
     * @param object Objeto a ser validado
     * @return Objeto validado
     * @throws Exception
     */

    @RequestMapping(value = "/validate", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public void validate(@RequestBody T object) {
        validator.validate(object);
    }

    /**
     * Valida um objeto apenas para os grupos informados.
     *
     * @param object Objeto a ser validado
     * @param groups Grupo de validação
     * @return Objeto validado
     * @throws Exception
     */

    @RequestMapping(value = "/validateGroup", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public void validateGroup(@RequestBody T object, Class<?>... groups) {
        validator.validate(object, groups);
    }

    /**
     * Remove um objeto pelo ID.
     *
     * @param id Identificador do objeto
     * @return Objeto removido.
     * @throws Exception
     */
    @DeleteMapping(value = "/")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public T removeById(@RequestBody ID id) {
        Optional<T> result = getRepository().findById(id);
        if (result.isPresent()) {
            getRepository().delete(result.get());
            return result.get();
        }
        throw new ArchbaseServiceException(messageSource, CONTROLLER_ENTIDADE_NOT_FOUND, id);
    }

    /**
     * Busca um objeto pelo seu ID.
     *
     * @param id Identificador do objeto.
     * @return Objeto encontrado.
     * @throws Exception
     */
    @GetMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public T findOne(@PathVariable(value = "id") ID id) {
        Optional<T> result = getRepository().findById(id);
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
    @PostMapping(value = "/findByComplexId")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public T findByComplexId(@RequestBody ID id) {
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
    @GetMapping(value = "/findAll", params = {"page", "size"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> findAll(@RequestParam("page") int page, @RequestParam("size") int size,
                           @RequestParam("fieldsToForceLazy") String fieldsToForceLazy) {
        Pageable pageable = PageRequest.of(page, size);
        Page<T> result = getRepository().findAll(pageable);
        Page<T> concretePage = this.createConcretePage(result.getContent(), pageable, result.getTotalElements());
        if (concretePage != null) {
            return concretePage;
        }
        return result;
    }

    /**
     * Busca os objetos da classe com paginação e ordenado
     *
     * @param page Número da página
     * @param size Tamanho da página
     * @param sort Campos para ordenação
     * @return Página
     */
    @GetMapping(value = "/findAll", params = {"page", "size", "sort"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> findAll(@RequestParam("page") int page, @RequestParam("size") int size,
                           @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<T> result = getRepository().findAll(pageable);
        Page<T> concretePage = this.createConcretePage(result.getContent(), pageable, result.getTotalElements());
        if (concretePage != null) {
            return concretePage;
        }
        return result;
    }

    /**
     * Busca os objetos da classe contido na lista de ID's.
     *
     * @param ids Lista de ID's
     * @return Lista de objetos encontrados.
     */
    @GetMapping(value = "/findAll", params = {"ids"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public List<T> findAll(@RequestParam(required = true) List<ID> ids) {
        List<T> result = getRepository().findAllById(ids);
        List<T> concreteList = this.createConcreteList(result);
        if (concreteList != null) {
            return concreteList;
        }
        return result;
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
    @GetMapping(value = "/findWithFilter", params = {"page", "size",
            "filter"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> find(@RequestParam(value = "filter", required = true) String filter, @RequestParam(value = "page", required = true) int page,
                        @RequestParam(value = "size", required = true) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<T> result = getRepository().findAll(filter, pageable);
        Page<T> concretePage = this.createConcretePage(result.getContent(), pageable, result.getTotalElements());
        if (concretePage != null) {
            return concretePage;
        }
        return result;
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
    @GetMapping(value = "/findWithFilterAndSort", params = {"page", "size",
            "filter", "sort"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<T> find(@RequestParam(value = "filter", required = true) String filter, @RequestParam(value = "page", required = true) int page,
                        @RequestParam(value = "size", required = true) int size,
                        @RequestParam(value = "sort", required = true) String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<T> result = getRepository().findAll(filter, pageable);
        Page<T> concretePage = this.createConcretePage(result.getContent(), pageable, result.getTotalElements());
        if (concretePage != null) {
            return concretePage;
        }
        return result;
    }

    /**
     * Count
     */

    /**
     * Retorna a quantidade de objetos da classe.
     *
     * @return Número total de objetos
     */
    @GetMapping(value = "/count")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
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
    @GetMapping(value = "/exists/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public boolean exists(@PathVariable ID id) {
        return getRepository().existsById(id);
    }

    /**
     * Verifica a existência de um objeto pelo seu id complexo.
     *
     * @param id Id do objeto
     * @return Verdadeiro se existir algum id.
     */
    @PostMapping(value = "/existsByComplexId")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public boolean existsByComplexId(@RequestBody ID id) {
        return getRepository().existsById(id);
    }

    protected abstract Page<T> createConcretePage(List<T> content, Pageable pageRequest, long totalElements);

    protected abstract List<T> createConcreteList(List<T> result);


}
