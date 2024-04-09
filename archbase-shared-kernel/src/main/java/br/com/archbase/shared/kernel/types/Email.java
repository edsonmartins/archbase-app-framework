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

    private boolean validateEmailAddress(String emailAddress) {
        // Implemente a lógica de validação do endereço de e-mail aqui
        // Você pode usar expressões regulares ou outras técnicas para verificar o formato do e-mail
        // Retorne true se o endereço de e-mail for válido, caso contrário, retorne false
        return true; // Substitua por sua lógica de validação real
    }
}

