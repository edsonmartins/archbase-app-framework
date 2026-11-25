package br.com.archbase.ddd.infraestructure.service;


import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.ddd.infraestructure.exceptions.ArchbaseServiceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;
import java.io.Serializable;
import java.util.Optional;

import static br.com.archbase.ddd.infraestructure.constants.DddConstantsErrorCodes.CONTROLLER_ENTIDADE_NOT_FOUND;

public abstract class SimpleArchbaseService<T, ID extends Serializable, N extends Number & Comparable<N>> extends SimpleArchbaseQueryService<T, ID, N> {

    @Autowired
    private MessageSource messageSource;
    /**
     * O {@code validator} do Archbase ({@code archbase-starter-core}), quando registrado. Com o Spring Boot 4 o
     * {@code defaultValidator} do Boot costuma vir antes e o do Archbase não é criado; até a 3.5.0 a injeção exigia
     * este nome e a aplicação não subia. Sem ele, vale o {@code Validator} do contexto.
     */
    @Autowired(required = false)
    @Qualifier("validator")
    private Validator validator;
    @Autowired
    private ObjectProvider<Validator> validadores;

    protected SimpleArchbaseService(Repository<T, ID, N> repository) {
        super(repository);
    }

    /**
     * Valida e insere ou atualiza um objeto.
     *
     * @param object Objeto a ser salvo
     * @return Objeto salvo
     * @throws jakarta.validation.ConstraintViolationException se houver violação; nada é gravado
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
    public T save(T object) {
        verificarRestricoes(object);
        return getRepository().save(object);
    }

    /**
     * Valida um objeto.
     *
     * @param object Objeto a ser validado
     * @throws jakarta.validation.ConstraintViolationException se houver violação
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public void validate(T object) {
        verificarRestricoes(object);
    }

    /**
     * Valida um objeto apenas para os grupos informados.
     *
     * @param object Objeto a ser validado
     * @param groups Grupo de validação
     * @throws jakarta.validation.ConstraintViolationException se houver violação
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
    public void validateGroup(T object, Class<?>... groups) {
        verificarRestricoes(object, groups);
    }

    /**
     * Confere as restrições de Bean Validation do objeto. Chamado por {@code save}, {@code validate} e
     * {@code validateGroup}.
     *
     * <p>Até a 3.5.0 o resultado era descartado e nada era recusado. Uma subclasse que dependa disso pode
     * sobrescrever este método.
     *
     * @throws jakarta.validation.ConstraintViolationException se houver violação
     */
    protected void verificarRestricoes(T object, Class<?>... groups) {
        RestricoesDoObjeto.exigir(RestricoesDoObjeto.escolher(validator, validadores), object, groups);
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
