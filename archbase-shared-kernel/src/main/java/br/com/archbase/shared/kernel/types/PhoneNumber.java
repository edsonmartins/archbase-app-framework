package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import br.com.archbase.ddd.domain.contracts.ValueObject;

import java.util.Objects;

@DomainValueObject
public class PhoneNumber implements ValueObject {

    private final String number;

    protected PhoneNumber() {
        this.number = null;
    }

    private PhoneNumber(String number) {
        if (validatePhoneNumber(number)) {
            this.number = number;
        } else {
            throw new IllegalArgumentException("Número de telefone inválido");
        }
    }

    public static PhoneNumber instance(String number) {
        return new PhoneNumber(number);
    }

    public String getNumber() {
        return number;
    }

    private boolean validatePhoneNumber(String number) {
        Objects.requireNonNull(number, "Número do telefone não deve ser nulo"); // (1)
        var sb = new StringBuilder();
        char ch;
        for (int i = 0; i < number.length(); ++i) {
            ch = number.charAt(i);
            if (Character.isDigit(ch)) { // (2)
                sb.append(ch);
            } else if (!Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != '-' && ch != '.') { // (3)
                throw new IllegalArgumentException(number + " não é um número de telefone válido");
            }
        }
        if (sb.length() == 0) { // (4)
            throw new IllegalArgumentException("Número do telefone não pode estar vazio.");
        }
        return true; // Substitua por sua lógica de validação real
    }

    @Override
    public String toString() {
        return number;
    }
}
