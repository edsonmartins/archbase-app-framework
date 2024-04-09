package br.com.archbase.ddd.infraestructure.service;


import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.ddd.infraestructure.exceptions.ArchbaseServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;
import java.io.Serializable;
import java.util.Optional;

import static br.com.archbase.ddd.infraestructure.constants.DddConstantsErrorCodes.CONTROLLER_ENTIDADE_NOT_FOUND;

public abstract class CommonArchbaseCommandService<T, ID extends Serializable, N extends Number & Comparable<N>> {

    private Repository<T, ID, N> repository;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    @Qualifier("validator")
    private Validator validator;


    @Autowired
    protected CommonArchbaseCommandService(Repository<T, ID, N> repository) {
        this.repository = repository;
    }

    public Repository<T, ID, N> getRepository() {
        return repository;
    }

    /**
     * Insere ou atualiza um objeto.
     *
     * @param object Objeto a ser salvo
     * @return Objeto salvo
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public T save(T object) {
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
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public void validate(T object) {
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
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public void validateGroup(T object, Class<?>... groups) {
        validator.validate(object, groups);
    }

    /**
     * Remove um objeto pelo ID.
     *
     * @param id Identificador do objeto
     * @return Objeto removido.
     * @throws Exception
     */
    public T removeById(ID id) {
        Optional<T> result = getRepository().findById(id);
        if (result.isPresent()) {
            getRepository().delete(result.get());
            return result.get();
        }
        throw new ArchbaseServiceException(messageSource, CONTROLLER_ENTIDADE_NOT_FOUND, id);
    }


}
