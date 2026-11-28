package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.annotations.DomainValueObject;
        import br.com.archbase.ddd.domain.contracts.ValueObject;

@DomainValueObject
public class Email implements ValueObject {

    private final String emailAddress;

    protected Email() {
        this.emailAddress = null;
    }

    private Email(String emailAddress) {
        if (validateEmailAddress(emailAddress)) {
            this.emailAddress = emailAddress;
        } else {
            throw new IllegalArgumentException("Endereço de e-mail inválido");
        }
    }

    public static Email instance() {
        return new Email();
    }

    public static Email instance(String emailAddress) {
        return new Email(emailAddress);
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    private static final java.util.regex.Pattern FORMATO =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /**
     * E-mail com o formato conferido.
     *
     * @throws IllegalArgumentException se o formato for inválido
     */
    public static Email of(String emailAddress) {
        if (!formatoValido(emailAddress)) {
            throw new IllegalArgumentException("Endereço de e-mail inválido");
        }
        return new Email(emailAddress);
    }

    /** Confere o formato ({@code nome@dominio.tld}). */
    public boolean isValido() {
        return formatoValido(emailAddress);
    }

    private static boolean formatoValido(String emailAddress) {
        return emailAddress != null && FORMATO.matcher(emailAddress.trim()).matches();
    }

    /**
     * {@code instance(...)} não confere o formato — continua aceitando o que sempre aceitou. Use {@link #of(String)} para
     * recusar endereço inválido. Até a 3.5.0 não havia validação nenhuma.
     */
    private boolean validateEmailAddress(String emailAddress) {
        return true;
    }
}

