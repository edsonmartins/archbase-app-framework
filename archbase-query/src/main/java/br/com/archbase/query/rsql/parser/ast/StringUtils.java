package br.com.archbase.query.rsql.parser.ast;

import java.util.List;

abstract interface StringUtils {

    public static String join(List<?> list, String glue) {

        StringBuilder line = new StringBuilder();
        for (Object s : list) {
            line.append(s).append(glue);
        }
        return list.isEmpty() ? "" : line.substring(0, line.length() - glue.length());
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
