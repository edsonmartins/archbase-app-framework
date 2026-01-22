package br.com.archbase.valueobject;

/**
 * Interface base para Value Objects.
 * <p>
 * Value Objects são objetos imutáveis identificados por seus atributos,
 * não por um ID. São fundamentais em Domain-Driven Design.
 * <p>
 * Características:
 * <ul>
 *   <li>Imutabilidade: não mudam após a criação</li>
 *   <li>Igualdade por valor: dois VOs são iguais se todos os atributos forem iguais</li>
 *   <li>Auto-validação: nunca existem em estado inválido</li>
 * </ul>
 */
public interface ValueObject {

    /**
     * Value Objects devem ser imutáveis e iguais por valor.
     * Implementações devem sobrescrever equals() e hashCode()
     * baseando-se em todos os seus atributos.
     */
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
