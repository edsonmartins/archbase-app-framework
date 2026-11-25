package br.com.archbase.valueobject.document;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Value Object para CNPJ (Cadastro Nacional da Pessoa Jurídica), numérico ou alfanumérico.
 * <p>
 * Desde julho de 2026 a Receita Federal emite CNPJ alfanumérico: as 12 primeiras posições aceitam letras
 * maiúsculas e dígitos, e os dois dígitos verificadores continuam numéricos. O cálculo é o mesmo módulo 11, com
 * cada caractere valendo o seu código ASCII menos 48 (dígitos de 0 a 9, {@code A} = 17 ... {@code Z} = 42). O CNPJ
 * numérico é um caso particular e continua válido.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * CNPJ cnpj = CNPJ.of("11.222.333/0001-81");
 * String numero = cnpj.numero(); // "11.222.333/0001-81"
 * CNPJ novo = CNPJ.of("12.ABC.345/01DE-35");
 * }
 * </pre>
 */
@Embeddable
public class CNPJ implements ValueObject {

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
     * @param numero Número do CNPJ (com ou sem formatação; letras minúsculas são convertidas)
     */
    public CNPJ(String numero) {
        if (numero == null) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo");
        }
        String numeroLimpo = limpar(numero);
        if (!numeroLimpo.matches("[0-9A-Z]{12}\\d{2}")) {
            throw new IllegalArgumentException(
                    "CNPJ deve conter 12 posições de letras ou dígitos e 2 dígitos verificadores");
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

        // CNPJ com todos os caracteres iguais é inválido
        if (cnpj.matches("(.)\\1{13}")) {
            return false;
        }

        String base = cnpj.substring(0, 12);
        int digito1 = calcularDigitoVerificador(base);
        int digito2 = calcularDigitoVerificador(base + digito1);

        return cnpj.equals(base + digito1 + digito2);
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

    /** Remove a máscara (e qualquer outro caractere que não seja letra ou dígito) e passa as letras para maiúsculas. */
    private static String limpar(String cnpj) {
        return cnpj.replaceAll("[^0-9A-Za-z]", "").toUpperCase(java.util.Locale.ROOT);
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

    /**
     * Módulo 11. Os pesos são alinhados pela direita: o primeiro dígito (12 posições) usa 5,4,3,2,9..2 e o segundo
     * (13 posições) usa 6,5,4,3,2,9..2. Cada caractere vale {@code c - '0'}.
     */
    private static int calcularDigitoVerificador(String base) {
        int soma = 0;
        int deslocamento = PESO_CNPJ.length - base.length();
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * PESO_CNPJ[i + deslocamento];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
