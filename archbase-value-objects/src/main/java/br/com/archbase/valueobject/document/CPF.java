package br.com.archbase.valueobject.document;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Value Object para CPF (Cadastro de Pessoas Físicas).
 * <p>
 * Uso:
 * <pre>
 * {@code
 * CPF cpf = new CPF("12345678909");
 * String numero = cpf.numero(); // "123.456.789-09"
 * boolean valido = cpf.valido();
 * }
 * </pre>
 */
@Embeddable
public class CPF implements ValueObject {

    private static final int TAMANHO = 11;
    private static final int[] PESO_CPF = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    @NotNull
    @Size(min = 11, max = 14)
    @Column(name = "cpf", length = 14)
    private String numero;

    /**
     * Construtor padrão para JPA.
     */
    protected CPF() {
    }

    /**
     * Construtor principal.
     *
     * @param numero Número do CPF (com ou sem formatação)
     */
    public CPF(String numero) {
        if (numero == null) {
            throw new IllegalArgumentException("CPF não pode ser nulo");
        }
        String numeroLimpo = limpar(numero);
        if (!numeroLimpo.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos");
        }
        this.numero = numeroLimpo;
    }

    /**
     * Cria um CPF validado.
     *
     * @param numero Número do CPF
     * @return CPF instância
     * @throws IllegalArgumentException se CPF for inválido
     */
    public static CPF of(String numero) {
        CPF cpf = new CPF(numero);
        if (!cpf.valido()) {
            throw new IllegalArgumentException("CPF inválido: " + numero);
        }
        return cpf;
    }

    /**
     * Cria um CPF sem validação dos dígitos verificadores.
     *
     * @param numero Número do CPF
     * @return CPF instância
     */
    public static CPF ofSemValidacao(String numero) {
        return new CPF(numero);
    }

    /**
     * Retorna o número do CPF formatado.
     *
     * @return CPF no formato XXX.XXX.XXX-XX
     */
    public String numero() {
        return formatar(numero);
    }

    /**
     * Retorna o número limpo do CPF.
     *
     * @return CPF apenas com dígitos
     */
    public String numeroLimpo() {
        return numero;
    }

    /**
     * Valida o CPF de acordo com os dígitos verificadores.
     *
     * @return true se válido, false caso contrário
     */
    public boolean valido() {
        String cpf = numero;

        // CPF com todos os dígitos iguais é inválido
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Calcula primeiro dígito verificador
        int digito1 = calcularDigitoVerificador(cpf.substring(0, 9), PESO_CPF);
        int digito2 = calcularDigitoVerificador(cpf.substring(0, 9) + digito1, PESO_CPF);

        return cpf.equals(cpf.substring(0, 9) + digito1 + digito2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CPF cpf = (CPF) o;
        return Objects.equals(numero, cpf.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return formatar(numero);
    }

    private static String limpar(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    private static String formatar(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." +
                cpf.substring(3, 6) + "." +
                cpf.substring(6, 9) + "-" +
                cpf.substring(9, 11);
    }

    private static int calcularDigitoVerificador(String base, int[] peso) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Integer.parseInt(base.substring(i, i + 1)) * peso[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
