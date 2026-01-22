package br.com.archbase.shared.ddd.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de uma validação.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * // Validado com sucesso
 * ValidationResult success = ValidationResult.success();
 * if (success.isValid()) { ... }
 *
 * // Com erros
 * ValidationResult failure = ValidationResult.failure("Campo inválido");
 * if (!failure.isValid()) {
 *     failure.getErrors().forEach(System.out::println);
 * }
 * }
 * </pre>
 */
public final class ValidationResult {

    private static final ValidationResult SUCCESS = new ValidationResult(Collections.emptyList());

    private final List<String> errors;

    private ValidationResult(List<String> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Cria um resultado de validação bem-sucedida.
     *
     * @return ValidationResult válido
     */
    public static ValidationResult success() {
        return SUCCESS;
    }

    /**
     * Cria um resultado de validação com erros.
     *
     * @param errors Lista de mensagens de erro
     * @return ValidationResult inválido
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(errors);
    }

    /**
     * Cria um resultado de validação com um único erro.
     *
     * @param error Mensagem de erro
     * @return ValidationResult inválido
     */
    public static ValidationResult failure(String error) {
        return new ValidationResult(Collections.singletonList(error));
    }

    /**
     * Cria um resultado de validação com múltiplos erros.
     *
     * @param errors Mensagens de erro
     * @return ValidationResult inválido
     */
    public static ValidationResult failure(String... errors) {
        return new ValidationResult(Arrays.asList(errors));
    }

    /**
     * Combina múltiplos resultados de validação.
     *
     * @param results Resultados a combinar
     * @return Resultado combinado
     */
    public static ValidationResult combine(ValidationResult... results) {
        List<String> allErrors = new ArrayList<>();
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                allErrors.addAll(result.getErrors());
            }
        }
        return allErrors.isEmpty() ? success() : failure(allErrors);
    }

    /**
     * Verifica se a validação foi bem-sucedida.
     *
     * @return true se válida, false caso contrário
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Verifica se a validação falhou.
     *
     * @return true se inválida, false caso contrário
     */
    public boolean isInvalid() {
        return !isValid();
    }

    /**
     * Retorna a lista de erros de validação.
     *
     * @return Lista imutável de erros
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Retorna a quantidade de erros.
     *
     * @return Quantidade de erros
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Adiciona um erro a este resultado.
     *
     * @param error Mensagem de erro
     * @return Novo ValidationResult com o erro adicionado
     */
    public ValidationResult withError(String error) {
        List<String> newErrors = new ArrayList<>(this.errors);
        newErrors.add(error);
        return new ValidationResult(newErrors);
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=false, errors=" + errors + "}";
    }
}
