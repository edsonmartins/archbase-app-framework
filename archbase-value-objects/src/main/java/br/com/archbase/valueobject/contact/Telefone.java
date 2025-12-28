package br.com.archbase.valueobject.contact;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object para Telefone.
 * <p>
 * Suporta telefones fixos e móveis no formato brasileiro.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * Telefone telefone = new Telefone("11987654321", TipoTelefone.CELULAR);
 * String formatado = telefone.formatado(); // "(11) 98765-4321"
 * }
 * </pre>
 */
@Embeddable
public class Telefone implements ValueObject {

    @Column(name = "tipo_telefone", length = 20)
    private TipoTelefone tipo;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "ddd", length = 3)
    private String ddd;

    /**
     * Construtor padrão para JPA.
     */
    protected Telefone() {
    }

    /**
     * Construtor principal.
     *
     * @param numero Número completo com DDD (ex: 11987654321)
     * @param tipo   Tipo do telefone
     */
    public Telefone(String numero, TipoTelefone tipo) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Número não pode ser vazio");
        }
        String limpo = limpar(numero);
        if (limpo.length() < 10 || limpo.length() > 11) {
            throw new IllegalArgumentException("Número deve ter 10 ou 11 dígitos (com DDD)");
        }

        this.ddd = limpo.substring(0, 2);
        this.numero = limpo.substring(2);
        this.tipo = tipo != null ? tipo : TipoTelefone.CELULAR;
    }

    /**
     * Construtor com DDD separado.
     *
     * @param ddd    DDD (ex: 11)
     * @param numero Número sem DDD
     * @param tipo   Tipo do telefone
     */
    public Telefone(String ddd, String numero, TipoTelefone tipo) {
        if (ddd == null || ddd.isBlank()) {
            throw new IllegalArgumentException("DDD não pode ser vazio");
        }
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Número não pode ser vazio");
        }

        String dddLimpo = limpar(ddd);
        String numeroLimpo = limpar(numero);

        if (dddLimpo.length() != 2) {
            throw new IllegalArgumentException("DDD deve ter 2 dígitos");
        }
        if (numeroLimpo.length() < 8 || numeroLimpo.length() > 9) {
            throw new IllegalArgumentException("Número deve ter 8 ou 9 dígitos");
        }

        this.ddd = dddLimpo;
        this.numero = numeroLimpo;
        this.tipo = tipo != null ? tipo : TipoTelefone.CELULAR;
    }

    /**
     * Cria um Telefone.
     *
     * @param numero Número completo com DDD
     * @param tipo   Tipo do telefone
     * @return Telefone instância
     */
    public static Telefone of(String numero, TipoTelefone tipo) {
        return new Telefone(numero, tipo);
    }

    /**
     * Cria um telefone celular.
     *
     * @param numero Número completo com DDD
     * @return Telefone instância
     */
    public static Telefone celular(String numero) {
        return new Telefone(numero, TipoTelefone.CELULAR);
    }

    /**
     * Cria um telefone fixo.
     *
     * @param numero Número completo com DDD
     * @return Telefone instância
     */
    public static Telefone fixo(String numero) {
        return new Telefone(numero, TipoTelefone.FIXO);
    }

    /**
     * Retorna o DDD.
     *
     * @return DDD
     */
    public String ddd() {
        return ddd;
    }

    /**
     * Retorna o número sem DDD.
     *
     * @return Número
     */
    public String numero() {
        return numero;
    }

    /**
     * Retorna o tipo do telefone.
     *
     * @return Tipo do telefone
     */
    public TipoTelefone tipo() {
        return tipo;
    }

    /**
     * Retorna o número completo (sem formatação).
     *
     * @return Número completo
     */
    public String numeroCompleto() {
        return ddd + numero;
    }

    /**
     * Retorna o número formatado.
     *
     * @return Número no formato (XX) XXXXX-XXXX ou (XX) XXXX-XXXX
     */
    public String formatado() {
        if (numero == null || ddd == null) {
            return null;
        }
        if (numero.length() == 9) {
            return String.format("(%s) %s-%s", ddd, numero.substring(0, 5), numero.substring(5, 9));
        } else {
            return String.format("(%s) %s-%s", ddd, numero.substring(0, 4), numero.substring(4, 8));
        }
    }

    /**
     * Verifica se é um número de celular.
     *
     * @return true se for celular
     */
    public boolean isCelular() {
        return tipo == TipoTelefone.CELULAR || (numero != null && numero.length() == 9);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Telefone telefone = (Telefone) o;
        return Objects.equals(numeroCompleto(), telefone.numeroCompleto());
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroCompleto());
    }

    @Override
    public String toString() {
        return formatado();
    }

    private static String limpar(String telefone) {
        return telefone.replaceAll("\\D", "");
    }

    /**
     * Tipo de telefone.
     */
    public enum TipoTelefone {
        CELULAR,
        FIXO,
        COMERCIAL,
        RESIDENCIAL,
        RECADO,
        OUTRO
    }
}
