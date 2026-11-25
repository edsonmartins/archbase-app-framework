package br.com.archbase.ddd.infraestructure.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;

/**
 * Validação comum aos services genéricos.
 *
 * <p>Até a 3.5.0 {@code save}, {@code validate} e {@code validateGroup} chamavam {@code validator.validate(...)} e
 * descartavam o resultado: nada era recusado. Agora uma violação lança {@link ConstraintViolationException}, a mesma
 * exceção que o Hibernate lança ao gravar uma entidade inválida — só que antes de tocar no banco, e sem vir embrulhada
 * na {@code TransactionSystemException} do commit.
 */
final class RestricoesDoObjeto {

    private RestricoesDoObjeto() {
    }

    /** O {@code validator} nomeado, se houver; senão, o {@code Validator} do contexto (respeitando {@code @Primary}). */
    static Validator escolher(Validator nomeado, ObjectProvider<Validator> outros) {
        if (nomeado != null) {
            return nomeado;
        }
        if (outros == null) {
            throw new IllegalStateException("Nenhum jakarta.validation.Validator disponível");
        }
        return outros.getObject();
    }

    static <T> void exigir(Validator validator, T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violacoes = validator.validate(object, groups);
        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException(violacoes);
        }
    }
}
