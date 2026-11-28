package br.com.archbase.ddd.domain.specification.basic;

import java.util.regex.Pattern;

/**
 * {@code like} do SQL avaliado em memória: {@code %} casa qualquer sequência, {@code _} um caractere, o resto é
 * literal e o texto precisa casar por inteiro. Valor {@code null} não casa — nem com {@code like} nem com
 * {@code not like}, como no banco.
 */
final class PadraoLike {

    private PadraoLike() {
    }

    /** {@code true}/{@code false} para o {@code like}; {@code null} se o candidato for nulo. */
    static Boolean casa(Object padrao, Object candidato) {
        if (candidato == null || padrao == null) {
            return null;
        }
        return regex(padrao.toString()).matcher(candidato.toString()).matches();
    }

    private static Pattern regex(String padrao) {
        StringBuilder sb = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        for (char c : padrao.toCharArray()) {
            if (c == '%' || c == '_') {
                if (!literal.isEmpty()) {
                    sb.append(Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                sb.append(c == '%' ? ".*" : ".");
            } else {
                literal.append(c);
            }
        }
        if (!literal.isEmpty()) {
            sb.append(Pattern.quote(literal.toString()));
        }
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }
}
