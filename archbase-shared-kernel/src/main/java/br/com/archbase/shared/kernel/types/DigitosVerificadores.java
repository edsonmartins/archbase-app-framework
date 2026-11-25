package br.com.archbase.shared.kernel.types;

/**
 * Módulo 11 de CPF e CNPJ (numérico e alfanumérico). Recebe o documento sem máscara.
 * <p>
 * No CNPJ alfanumérico, emitido pela Receita Federal desde julho de 2026, as 12 primeiras posições aceitam letras
 * maiúsculas; cada caractere vale o seu código ASCII menos 48 ({@code 0}..{@code 9} = 0..9, {@code A} = 17 ...
 * {@code Z} = 42). Os dois dígitos verificadores continuam numéricos.
 */
final class DigitosVerificadores {

    private static final int[] PESO_CNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private DigitosVerificadores() {
    }

    static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        String base = cpf.substring(0, 9);
        int digito1 = moduloOnze(somaCpf(base));
        int digito2 = moduloOnze(somaCpf(base + digito1));
        return cpf.equals(base + digito1 + digito2);
    }

    static boolean cnpjValido(String cnpj) {
        if (cnpj == null || !cnpj.matches("[0-9A-Z]{12}\\d{2}") || cnpj.matches("(.)\\1{13}")) {
            return false;
        }
        String base = cnpj.substring(0, 12);
        int digito1 = moduloOnze(somaCnpj(base));
        int digito2 = moduloOnze(somaCnpj(base + digito1));
        return cnpj.equals(base + digito1 + digito2);
    }

    /** Pesos de {@code tamanho + 1} até 2: 10..2 no primeiro dígito, 11..2 no segundo. */
    private static int somaCpf(String base) {
        int soma = 0;
        int peso = base.length() + 1;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * peso--;
        }
        return soma;
    }

    /** Pesos alinhados pela direita: 5,4,3,2,9..2 no primeiro dígito, 6,5,4,3,2,9..2 no segundo. */
    private static int somaCnpj(String base) {
        int soma = 0;
        int deslocamento = PESO_CNPJ.length - base.length();
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * PESO_CNPJ[i + deslocamento];
        }
        return soma;
    }

    private static int moduloOnze(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
