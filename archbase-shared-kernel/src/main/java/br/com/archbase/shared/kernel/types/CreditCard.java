package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import br.com.archbase.ddd.domain.contracts.ValueObject;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.MaskFormatter;
import jakarta.validation.constraints.Size;
import java.text.ParseException;

@DomainValueObject
public class CreditCard  implements ValueObject {

    public static final String MASK = "####-####-####-####";

    @Size(max = 19)
    private final String cardNumber;

    @Size(min = 3, max = 4)
    private final String securityCode;

    @Size(min = 1, max = 2)
    private final String expirationMonth;

    @Size(min = 2, max = 2)
    private final String expirationYear;

    protected CreditCard() {
        this.cardNumber = null;
        this.securityCode = null;
        this.expirationMonth = null;
        this.expirationYear = null;
    }

    private CreditCard(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        if (validateCreditCard(cardNumber, securityCode, expirationMonth, expirationYear)) {
            this.cardNumber = cardNumber;
            this.securityCode = securityCode;
            this.expirationMonth = expirationMonth;
            this.expirationYear = expirationYear;
        } else {
            throw new IllegalArgumentException("Dados do cartão de crédito inválidos");
        }
    }

    public static CreditCard instance() {
        return new CreditCard();
    }

    public static CreditCard instance(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        return new CreditCard(cardNumber, securityCode, expirationMonth, expirationYear);
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public String getFormattedCard() {
        return applyCardMask();
    }

    private boolean validateCreditCard(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        // Implemente a lógica de validação do cartão de crédito aqui
        // Você pode usar algoritmos para verificar o número do cartão, data de expiração, código de segurança, etc.
        // Retorne true se os dados forem válidos, caso contrário, retorne false
        return true; // Substitua por sua lógica de validação real
    }

    private String applyCardMask() {
        try {
            if (StringUtils.isBlank(cardNumber)) {
                return null;
            }
            MaskFormatter mf = new MaskFormatter(MASK);
            mf.setValueContainsLiteralCharacters(false);
            return mf.valueToString(cardNumber);
        } catch (NullPointerException | ParseException ex) {
            return null;
        }
    }
}

