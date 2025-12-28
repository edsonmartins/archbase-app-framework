package br.com.archbase.shared.ddd.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Validador de regras de negócio.
 * <p>
 * Permite definir regras de negócio de forma fluente e expressiva.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * ValidationResult result = BusinessRuleValidator.validate()
 *     .rule(client.getNome() != null, "Nome é obrigatório")
 *     .rule(client.getIdade() >= 18, "Cliente deve ser maior de idade")
 *     .rule(() -> emailValido(client.getEmail()), "Email inválido")
 *     .validate();
 * }
 * </pre>
 */
public final class BusinessRuleValidator {

    private final List<Rule> rules = new ArrayList<>();

    private BusinessRuleValidator() {
    }

    /**
     * Cria uma nova instância do validador.
     *
     * @return Nova instância
     */
    public static BusinessRuleValidator create() {
        return new BusinessRuleValidator();
    }

    /**
     * Adiciona uma regra de validação.
     *
     * @param condition Condição que deve ser verdadeira
     * @param errorMessage Mensagem de erro se a condição for falsa
     * @return Este validador para encadeamento
     */
    public BusinessRuleValidator rule(boolean condition, String errorMessage) {
        rules.add(new Rule(condition, errorMessage));
        return this;
    }

    /**
     * Adiciona uma regra de validação lazy.
     *
     * @param condition Fornecedor da condição
     * @param errorMessage Mensagem de erro
     * @return Este validador para encadeamento
     */
    public BusinessRuleValidator rule(BooleanSupplier condition, String errorMessage) {
        rules.add(new Rule(condition.getAsBoolean(), errorMessage));
        return this;
    }

    /**
     * Adiciona uma regra de validação com exceção customizada.
     *
     * @param condition Condição que deve ser verdadeira
     * @param exceptionSupplier Fornecedor da exceção
     * @param <T> Tipo da exceção
     * @return Este validador para encadeamento
     */
    public <T extends RuntimeException> BusinessRuleValidator rule(
            boolean condition,
            Supplier<T> exceptionSupplier) {
        if (!condition) {
            rules.add(new Rule(false, exceptionSupplier.get().getMessage()));
        }
        return this;
    }

    /**
     * Adiciona uma regra de validação que lança exceção se inválida.
     *
     * @param condition Condição que deve ser verdadeira
     * @param exceptionSupplier Fornecedor da exceção
     * @param <T> Tipo da exceção
     * @return Este validador para encadeamento
     * @throws T se a condição for falsa
     */
    public <T extends RuntimeException> BusinessRuleValidator ruleOrThrow(
            boolean condition,
            Supplier<T> exceptionSupplier) throws T {
        if (!condition) {
            throw exceptionSupplier.get();
        }
        return this;
    }

    /**
     * Valida todas as regras e retorna o resultado.
     *
     * @return Resultado da validação
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        for (Rule rule : rules) {
            if (!rule.valid) {
                errors.add(rule.errorMessage);
            }
        }
        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    /**
     * Valida e lança exceção se houver erros.
     *
     * @throws ValidationException se a validação falhar
     */
    public void validateOrThrow() throws ValidationException {
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new ValidationException(result.getErrors());
        }
    }

    /**
     * Adiciona uma lista de regras.
     *
     * @param rulesToAdd Regras a adicionar
     * @return Este validador para encadeamento
     */
    public BusinessRuleValidator rules(List<Rule> rulesToAdd) {
        rules.addAll(rulesToAdd);
        return this;
    }

    /**
     * Verifica se há alguma regra configurada.
     *
     * @return true se houver regras
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * Retorna a quantidade de regras configuradas.
     *
     * @return Quantidade de regras
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * Representa uma regra de validação.
     */
    public static class Rule {
        private final boolean valid;
        private final String errorMessage;

        public Rule(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Exceção lançada quando a validação de negócio falha.
     */
    public static class ValidationException extends RuntimeException {
        private final List<String> errors;

        public ValidationException(List<String> errors) {
            super("Validação falhou: " + String.join(", ", errors));
            this.errors = Collections.unmodifiableList(errors);
        }

        public ValidationException(String error) {
            this(Arrays.asList(error));
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
