package br.com.archbase.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * ModCheckBase contém todos os métodos e opções compartilhados usados por Mod Check Validators
 * <p>
 * http://en.wikipedia.org/wiki/Check_digit
 */
public abstract class ModCheckBase {

    private static final Pattern NUMBERS_ONLY_REGEXP = Pattern.compile("[^0-9]");

    private static final int DEC_RADIX = 10;

    /**
     * O índice inicial para o cálculo da soma de verificação
     */
    private int startIndex;

    /**
     * O índice final para o cálculo da soma de verificação
     */
    private int endIndex;

    /**
     * O índice do dígito da soma de verificação
     */
    private int checkDigitIndex;

    private boolean ignoreNonDigitCharacters;

    public boolean isValid(final CharSequence value) {
        if (value == null) {
            return true;
        }

        String valueAsString = value.toString();
        String digitsAsString;
        char checkDigit;
        try {
            digitsAsString = extractVerificationString(valueAsString);
            checkDigit = extractCheckDigit(valueAsString);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
        digitsAsString = stripNonDigitsIfRequired(digitsAsString);

        List<Integer> digits;
        try {
            digits = extractDigits(digitsAsString);
        } catch (NumberFormatException e) {
            return false;
        }

        return this.isCheckDigitValid(digits, checkDigit);
    }

    public abstract boolean isCheckDigitValid(List<Integer> digits, char checkDigit);

    protected void initialize(int startIndex, int endIndex, int checkDigitIndex, boolean ignoreNonDigitCharacters) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.checkDigitIndex = checkDigitIndex;
        this.ignoreNonDigitCharacters = ignoreNonDigitCharacters;

        this.validateOptions();
    }

    /**
     * Retorna o valor numérico {@code int} de um {@code char}
     *
     * @param value a entrada {@code char} a ser analisada
     * @return o valor numérico {@code int} representado pelo caractere.
     * @throws NumberFormatException caso o caractere não seja um dígito
     */
    protected int extractDigit(char value) {
        if (Character.isDigit(value)) {
            return Character.digit(value, DEC_RADIX);
        } else {
            throw new NumberFormatException("'" + value + "' is not a digit.");
        }
    }

    /**
     * Analisa o valor {@link String} como uma {@link List} de objetos {@link Integer}
     *
     * @param value a string de entrada a ser analisada
     * @return Lista de objetos {@code Integer}.
     * @throws NumberFormatException no caso de algum dos caracteres não ser um dígito
     */
    private List<Integer> extractDigits(final String value) {
        List<Integer> digits = new ArrayList<>(value.length());
        char[] chars = value.toCharArray();
        for (char c : chars) {
            digits.add(extractDigit(c));
        }
        return digits;
    }

    private boolean validateOptions() {
        if (this.startIndex < 0) {
            throw new IllegalArgumentException("Índice inicial não pode ser negativo: " + this.startIndex);
        }

        if (this.endIndex < 0) {
            throw new IllegalArgumentException("Índice final não pode ser negativo: " + this.endIndex);
        }

        if (this.startIndex > this.endIndex) {
            throw new IllegalArgumentException("Intervalo inválido: " + startIndex + " > " + endIndex);
        }

        if (this.checkDigitIndex > 0 && this.startIndex <= this.checkDigitIndex && this.endIndex > this.checkDigitIndex) {
            throw new IllegalArgumentException("Um dígito de verificação especificado explicitamente deve estar fora do intervalo: [" + startIndex + ", " + endIndex + "].");
        }

        return true;
    }

    private String stripNonDigitsIfRequired(String value) {
        if (ignoreNonDigitCharacters) {
            return NUMBERS_ONLY_REGEXP.matcher(value).replaceAll("");
        } else {
            return value;
        }
    }

    private String extractVerificationString(String value) {
        // a string contém o dígito de verificação, basta retornar os dígitos para verificar
        if (endIndex == Integer.MAX_VALUE) {
            return value.substring(0, value.length() - 1);
        } else if (checkDigitIndex == -1) {
            return value.substring(startIndex, endIndex);
        } else {
            return value.substring(startIndex, endIndex + 1);
        }
    }

    private char extractCheckDigit(String value) {
        // pega o último caractere da string a ser validado a menos que o índice seja fornecido explicitamente
        if (checkDigitIndex == -1) {
            if (endIndex == Integer.MAX_VALUE) {
                return value.charAt(value.length() - 1);
            } else {
                return value.charAt(endIndex);
            }
        } else {
            return value.charAt(checkDigitIndex);
        }
    }

}
