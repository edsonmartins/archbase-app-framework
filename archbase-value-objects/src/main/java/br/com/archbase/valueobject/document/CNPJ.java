package br.com.archbase.valueobject.document;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Value Object para CNPJ (Cadastro Nacional da Pessoa Jurídica).
 * <p>
 * Uso:
 * <pre>
 * {@code
 * CNPJ cnpj = new CNPJ("12345678000190");
 * String numero = cnpj.numero(); // "12.345.678/0001-90"
 * boolean valido = cnpj.valido();
 * }
 * </pre>
 */
@Embeddable
public class CNPJ implements ValueObject {

    private static final int TAMANHO = 14;
    private static final int[] PESO_CNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @NotNull
    @Size(min = 14, max = 18)
    @Column(name = "cnpj", length = 18)
    private String numero;

    /**
     * Construtor padrão para JPA.
     */
    protected CNPJ() {
    }

    /**
     * Construtor principal.
     *
     * @param numero Número do CNPJ (com ou sem formatação)
     */
    public CNPJ(String numero) {
        if (numero == null) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo");
        }
        String numeroLimpo = limpar(numero);
        if (!numeroLimpo.matches("\\d{14}")) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos");
        }
        this.numero = numeroLimpo;
    }

    /**
     * Cria um CNPJ validado.
     *
     * @param numero Número do CNPJ
     * @return CNPJ instância
     * @throws IllegalArgumentException se CNPJ for inválido
     */
    public static CNPJ of(String numero) {
        CNPJ cnpj = new CNPJ(numero);
        if (!cnpj.valido()) {
            throw new IllegalArgumentException("CNPJ inválido: " + numero);
        }
        return cnpj;
    }

    /**
     * Cria um CNPJ sem validação dos dígitos verificadores.
     *
     * @param numero Número do CNPJ
     * @return CNPJ instância
     */
    public static CNPJ ofSemValidacao(String numero) {
        return new CNPJ(numero);
    }

    /**
     * Retorna o número do CNPJ formatado.
     *
     * @return CNPJ no formato XX.XXX.XXX/XXXX-XX
     */
    public String numero() {
        return formatar(numero);
    }

    /**
     * Retorna o número limpo do CNPJ.
     *
     * @return CNPJ apenas com dígitos
     */
    public String numeroLimpo() {
        return numero;
    }

    /**
     * Valida o CNPJ de acordo com os dígitos verificadores.
     *
     * @return true se válido, false caso contrário
     */
    public boolean valido() {
        String cnpj = numero;

        // CNPJ com todos os dígitos iguais é inválido
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // Calcula primeiro dígito verificador
        int digito1 = calcularDigitoVerificador(cnpj.substring(0, 12), PESO_CNPJ);
        // Calcula segundo dígito verificador
        int digito2 = calcularDigitoVerificador(cnpj.substring(0, 12) + digito1, PESO_CNPJ);

        return cnpj.equals(cnpj.substring(0, 12) + digito1 + digito2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CNPJ cnpj = (CNPJ) o;
        return Objects.equals(numero, cnpj.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return formatar(numero);
    }

    private static String limpar(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }

    private static String formatar(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." +
                cnpj.substring(2, 5) + "." +
                cnpj.substring(5, 8) + "/" +
                cnpj.substring(8, 12) + "-" +
                cnpj.substring(12, 14);
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
