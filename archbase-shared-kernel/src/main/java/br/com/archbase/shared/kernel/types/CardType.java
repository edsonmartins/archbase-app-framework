package br.com.archbase.shared.kernel.types;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public enum CardType {

    UNKNOWN,
    VISA("^4[0-9]{12}(?:[0-9]{3}){0,2}$"),
    MASTERCARD("^(?:5[1-5]|2(?!2([01]|20)|7(2[1-9]|3))[2-7])\\d{14}$"),
    AMERICAN_EXPRESS("^3[47][0-9]{13}$"),
    DINERS_CLUB("^3(?:0[0-5]\\d|095|6\\d{0,2}|[89]\\d{2})\\d{12,15}$"),
    DISCOVER("^6(?:011|[45][0-9]{2})[0-9]{12}$"),
    JCB("^(?:2131|1800|35\\d{3})\\d{11}$"),
    MAESTRO("^(5018|5020|5038|6304|6759|6761|6763)[0-9]{8,15}$"),
    HIPER("^637(095|568|599|609|612)\\\\d*"),
    HIPERCARD("^606282\\d*"),
    SOLO("^(6334|6767)[0-9]{12}|(6334|6767)[0-9]{14}|(6334|6767)[0-9]{15}$"),
    CARTE_BLANCHE("^389[0-9]{11}$"),
    VISA_MASTER("^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14})$"),
    INSTA_PAYMENT("^63[7-9][0-9]{13}$"),
    LASER("^(6304|6706|6709|6771)[0-9]{12,15}$"),
    SWITCH("^(4903|4905|4911|4936|6333|6759)[0-9]{12}|(4903|4905|4911|4936|6333|6759)[0-9]{14}|(4903|4905|4911|4936|6333|6759)[0-9]{15}|564182[0-9]{10}|564182[0-9]{12}|564182[0-9]{13}|633110[0-9]{10}|633110[0-9]{12}|633110[0-9]{13}$"),
    UNION_PAY("^(62[0-9]{14,17})$"),
    KOREAN_LOCAL("^9[0-9]{15}$"),
    BCGLOBAL("^(6541|6556)[0-9]{12}$"),
    CHINA_UNION_PAY("^62[0-9]{14,17}$");
    private Pattern pattern;

    CardType() {
        this.pattern = null;
    }

    CardType(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public static CardType detect(String cardNumber) {

        for (CardType cardType : CardType.values()) {
            if (null == cardType.pattern) continue;
            if (cardType.pattern.matcher(cardNumber).matches()) return cardType;
        }

        return UNKNOWN;
    }


    /**
     * Executa a verificação Luhn no número do cartão fornecido.
     *
     * @param cardNumber um String que consiste em dígitos numéricos (somente).
     * @return {@code true} se a sequência passar na soma de verificação
     * @throws IllegalArgumentException if {@code cardNumber} não contém um digito (onde {@link
     *                                  Character#isDefined(char)} is {@code false}).
     * @see <a href="http://en.wikipedia.org/wiki/Luhn_algorithm">Luhn Algorithm (Wikipedia)</a>
     */
    public static boolean isLuhnValid(String cardNumber) {
        final String reversed = new StringBuffer(cardNumber).reverse().toString();
        final int len = reversed.length();
        int oddSum = 0;
        int evenSum = 0;
        for (int i = 0; i < len; i++) {
            final char c = reversed.charAt(i);
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException(String.format("Não é um digito: '%s'", c));
            }
            final int digit = Character.digit(c, 10);
            if (i % 2 == 0) {
                oddSum += digit;
            } else {
                evenSum += digit / 5 + (2 * digit) % 10;
            }
        }
        return (oddSum + evenSum) % 10 == 0;
    }

    /**
     * @param cardNumber O número do cartão para validar
     * @return {@code true} se este número de cartão for válido.
     */
    public boolean validate(String cardNumber) {
        if ((StringUtils.isEmpty(cardNumber)) || (!StringUtils.isNumeric(cardNumber))) {
            return false;
        }
        return isLuhnValid(cardNumber);
    }

}
