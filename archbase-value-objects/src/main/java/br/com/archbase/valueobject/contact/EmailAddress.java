package br.com.archbase.valueobject.contact;

import br.com.archbase.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object para EmailAddress.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * EmailAddress email = new EmailAddress("usuario@exemplo.com");
 * String endereco = email.endereco(); // "usuario@exemplo.com"
 * String usuario = email.usuario(); // "usuario"
 * String dominio = email.dominio(); // "exemplo.com"
 * }
 * </pre>
 */
@Embeddable
public class EmailAddress implements ValueObject, Comparable<EmailAddress> {

    /**
     * Regex simplificado para validação de email.
     * Não aceita todos os emails válidos segundo RFC 5322, mas cobre a maioria dos casos.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    @NotBlank
    @Column(name = "email", length = 255)
    private String endereco;

    /**
     * Construtor padrão para JPA.
     */
    protected EmailAddress() {
    }

    /**
     * Construtor principal.
     *
     * @param endereco Endereço de email
     * @throws IllegalArgumentException se email for inválido
     */
    public EmailAddress(String endereco) {
        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException("Email não pode ser vazio");
        }
        String enderecoTrim = endereco.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(enderecoTrim).matches()) {
            throw new IllegalArgumentException("Email inválido: " + endereco);
        }
        this.endereco = enderecoTrim;
    }

    /**
     * Cria um EmailAddress validado.
     *
     * @param endereco Endereço de email
     * @return EmailAddress instância
     * @throws IllegalArgumentException se email for inválido
     */
    public static EmailAddress of(String endereco) {
        return new EmailAddress(endereco);
    }

    /**
     * Cria um EmailAddress sem validação.
     *
     * @param endereco Endereço de email
     * @return EmailAddress instância
     */
    public static EmailAddress ofSemValidacao(String endereco) {
        EmailAddress email = new EmailAddress();
        email.endereco = endereco != null ? endereco.trim().toLowerCase() : null;
        return email;
    }

    /**
     * Retorna o endereço de email.
     *
     * @return Endereço completo
     */
    public String endereco() {
        return endereco;
    }

    /**
     * Retorna a parte do usuário do email.
     *
     * @return Usuário (parte antes do @)
     */
    public String usuario() {
        if (endereco == null) return null;
        int arroba = endereco.indexOf('@');
        return arroba > 0 ? endereco.substring(0, arroba) : null;
    }

    /**
     * Retorna o domínio do email.
     *
     * @return Domínio (parte depois do @)
     */
    public String dominio() {
        if (endereco == null) return null;
        int arroba = endereco.indexOf('@');
        return arroba > 0 ? endereco.substring(arroba + 1) : null;
    }

    /**
     * Verifica se o email é de um domínio específico.
     *
     * @param dominio Domínio a verificar
     * @return true se o domínio corresponder
     */
    public boolean isDeDominio(String dominio) {
        return dominio != null && this.dominio() != null
                && this.dominio().equalsIgnoreCase(dominio);
    }

    /**
     * Verifica se o email é corporativo (não é de provedores públicos comuns).
     *
     * @return true se não for gmail, outlook, yahoo, etc.
     */
    public boolean isCorporativo() {
        String dom = this.dominio();
        if (dom == null) return false;
        return !dom.endsWith("gmail.com") &&
                !dom.endsWith("outlook.com") &&
                !dom.endsWith("hotmail.com") &&
                !dom.endsWith("yahoo.com") &&
                !dom.endsWith("icloud.com") &&
                !dom.endsWith("live.com");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress email = (EmailAddress) o;
        return Objects.equals(endereco, email.endereco);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endereco);
    }

    @Override
    public String toString() {
        return endereco;
    }

    @Override
    public int compareTo(EmailAddress other) {
        if (this.endereco == null && other.endereco == null) return 0;
        if (this.endereco == null) return -1;
        if (other.endereco == null) return 1;
        return this.endereco.compareTo(other.endereco);
    }
}
