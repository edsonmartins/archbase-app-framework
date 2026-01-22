package br.com.archbase.valueobject.address;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object para Endereço.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * Endereco endereco = new Endereco()
 *     .logradouro("Rua das Flores")
 *     .numero("123")
 *     .bairro("Centro")
 *     .cidade("São Paulo")
 *     .uf("SP")
 *     .cep("01234-567");
 * }
 * </pre>
 */
@Embeddable
public class Endereco implements ValueObject {

    @Column(name = "logradouro", length = 255)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "complemento", length = 255)
    private String complemento;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep", length = 9)
    private String cep;

    @Column(name = "pais", length = 50)
    private String pais;

    /**
     * Construtor padrão para JPA.
     */
    protected Endereco() {
        this.pais = "Brasil";
    }

    /**
     * Construtor completo.
     */
    private Endereco(String logradouro, String numero, String complemento,
                     String bairro, String cidade, String uf, String cep, String pais) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.cep = cep;
        this.pais = pais != null ? pais : "Brasil";
    }

    /**
     * Cria um novo builder para Endereco.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cria um Endereço com CEP apenas.
     *
     * @param cep CEP
     * @return Endereco instância
     */
    public static Endereco deCep(String cep) {
        return new Builder().cep(cep).build();
    }

    // Getters
    public String logradouro() {
        return logradouro;
    }

    public String numero() {
        return numero;
    }

    public String complemento() {
        return complemento;
    }

    public String bairro() {
        return bairro;
    }

    public String cidade() {
        return cidade;
    }

    public String uf() {
        return uf;
    }

    public String cep() {
        return cep;
    }

    public String pais() {
        return pais;
    }

    /**
     * Retorna o endereço formatado em uma linha.
     *
     * @return Endereço formatado
     */
    public String linhaUnica() {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null) {
            sb.append(logradouro);
        }
        if (numero != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(numero);
        }
        if (complemento != null && !complemento.isBlank()) {
            sb.append(" - ").append(complemento);
        }
        if (bairro != null) {
            sb.append(", ").append(bairro);
        }
        return sb.toString();
    }

    /**
     * Retorna cidade/UF.
     *
     * @return Cidade-UF
     */
    public String cidadeUf() {
        if (cidade == null && uf == null) {
            return null;
        }
        return cidade + "-" + uf;
    }

    /**
     * Verifica se o endereço está completo (tem todos os campos obrigatórios).
     *
     * @return true se completo
     */
    public boolean isCompleto() {
        return logradouro != null && !logradouro.isBlank() &&
                numero != null && !numero.isBlank() &&
                bairro != null && !bairro.isBlank() &&
                cidade != null && !cidade.isBlank() &&
                uf != null && !uf.isBlank() &&
                cep != null && !cep.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endereco endereco = (Endereco) o;
        return Objects.equals(logradouro, endereco.logradouro) &&
                Objects.equals(numero, endereco.numero) &&
                Objects.equals(complemento, endereco.complemento) &&
                Objects.equals(bairro, endereco.bairro) &&
                Objects.equals(cidade, endereco.cidade) &&
                Objects.equals(uf, endereco.uf) &&
                Objects.equals(cep, endereco.cep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logradouro, numero, complemento, bairro, cidade, uf, cep);
    }

    @Override
    public String toString() {
        return linhaUnica() + " - " + cidadeUf() + " (" + cep + ")";
    }

    /**
     * Builder para Endereco.
     */
    public static class Builder {
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String uf;
        private String cep;
        private String pais = "Brasil";

        public Builder logradouro(String logradouro) {
            this.logradouro = logradouro;
            return this;
        }

        public Builder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public Builder complemento(String complemento) {
            this.complemento = complemento;
            return this;
        }

        public Builder bairro(String bairro) {
            this.bairro = bairro;
            return this;
        }

        public Builder cidade(String cidade) {
            this.cidade = cidade;
            return this;
        }

        public Builder uf(String uf) {
            this.uf = uf;
            return this;
        }

        public Builder cep(String cep) {
            this.cep = cep;
            return this;
        }

        public Builder pais(String pais) {
            this.pais = pais;
            return this;
        }

        public Endereco build() {
            return new Endereco(logradouro, numero, complemento,
                    bairro, cidade, uf, cep, pais);
        }
    }
}
