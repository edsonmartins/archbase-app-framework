package br.com.archbase.shared.ddd.valueobject;

import br.com.archbase.shared.ddd.validation.BusinessRuleValidator;
import br.com.archbase.shared.ddd.validation.ValidationResult;

/**
 * Classe base para Value Objects.
 * <p>
 * Fornece métodos comuns para implementação de Value Objects.
 * <p>
 * Subclasses devem:
 * <ul>
 *   <li>Ser imutáveis</li>
 *   <li>Implementar equals() e hashCode() baseados em todos os campos</li>
 *   <li>Implementar validate() para auto-validação</li>
 * </ul>
 * <p>
 * Uso:
 * <pre>
 * {@code
 * public class Email extends BaseValueObject {
 *     private final String endereco;
 *
 *     public Email(String endereco) {
 *         this.endereco = endereco;
 *         validateOrThrow();
 *     }
 *
 *     @Override
 *     public ValidationResult validate() {
 *         return BusinessRuleValidator.create()
 *             .rule(endereco != null, "Email é obrigatório")
 *             .rule(endereco.contains("@"), "Email inválido")
 *             .validate();
 *     }
 *
 *     // getters, equals, hashCode, toString
 * }
 * }
 * </pre>
 */
public abstract class BaseValueObject {

    /**
     * Valida o Value Object.
     * <p>
     * Implementações devem retornar {@link ValidationResult#success()}
     * se o valor for válido, ou {@link ValidationResult#failure(String)}
     * com a mensagem de erro apropriada.
     *
     * @return Resultado da validação
     */
    public ValidationResult validate() {
        return ValidationResult.success();
    }

    /**
     * Valida e lança exceção se inválido.
     *
     * @throws BusinessRuleValidator.ValidationException se a validação falhar
     */
    protected void validateOrThrow() throws BusinessRuleValidator.ValidationException {
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new BusinessRuleValidator.ValidationException(result.getErrors());
        }
    }

    /**
     * Verifica se este Value Object é válido.
     *
     * @return true se válido
     */
    public boolean isValid() {
        return validate().isValid();
    }
}
