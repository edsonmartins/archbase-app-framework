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

    /**
     * Cartão com os dados conferidos: número pelo algoritmo de Luhn, mês de 1 a 12, ano com 2 dígitos e código de
     * segurança com 3 ou 4 dígitos.
     *
     * @throws IllegalArgumentException se algum dado for inválido
     */
    public static CreditCard of(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        if (!dadosValidos(cardNumber, securityCode, expirationMonth, expirationYear)) {
            throw new IllegalArgumentException("Dados do cartão de crédito inválidos");
        }
        return new CreditCard(cardNumber, securityCode, expirationMonth, expirationYear);
    }

    /** Confere os dados (ver {@link #of}). */
    public boolean isValido() {
        return dadosValidos(cardNumber, securityCode, expirationMonth, expirationYear);
    }

    private static boolean dadosValidos(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        if (cardNumber == null || securityCode == null || expirationMonth == null || expirationYear == null) {
            return false;
        }
        String numero = cardNumber.replaceAll("[\\s-]", "");
        if (!numero.matches("\\d{12,19}") || !CardType.isLuhnValid(numero)) {
            return false;
        }
        if (!securityCode.matches("\\d{3,4}") || !expirationYear.matches("\\d{2}") || !expirationMonth.matches("\\d{1,2}")) {
            return false;
        }
        int mes = Integer.parseInt(expirationMonth);
        return mes >= 1 && mes <= 12;
    }

    /**
     * {@code instance(...)} não confere os dados — continua aceitando o que sempre aceitou. Use {@link #of} para
     * recusar cartão inválido. Até a 3.5.0 não havia validação nenhuma.
     */
    private boolean validateCreditCard(String cardNumber, String securityCode, String expirationMonth, String expirationYear) {
        return true;
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

