package br.com.archbase.plugin.manager.util;


public class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String str) {
        return (str == null) || str.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    /**
     * Formate a string. Substitua "{}" por% se formate a string usando {@link String #format(String, Object ...)}.
     */
    public static String format(String str, Object... args) {
        str = str.replace("\\{}", "%s");
        return String.format(str, args);
    }

    /**
     * <p> Adiciona uma substring apenas se a string de origem ainda não começar com a substring,
     * caso contrário, retorna a string de origem. </p>
     * <p />
     * <p> Uma string de origem {@code null} retornará {@code null}.
     * Uma string de origem vazia ("") retornará a string vazia.
     * Uma string de pesquisa {@code null} retornará a string de origem. </p>
     * <p/>
     * <pre>
     * StringUtils.addStart (null, *) = *
     * StringUtils.addStart ("", *) = *
     * StringUtils.addStart (*, null) = *
     * StringUtils.addStart ("dominio.com", "www.") = "Www.domain.com"
     * StringUtils.addStart ("abc123", "abc") = "abc123"
     * </pre>
     *
     * @param str a string de origem a pesquisar, pode ser nulo
     * @param add a string para pesquisar e adicionar, pode ser nulo
     * @retorne a substring com a string adicionada, se necessário
     */
    public static String addStart(String str, String add) {
        if (isNullOrEmpty(add)) {
            return str;
        }

        if (isNullOrEmpty(str)) {
            return add;
        }

        if (!str.startsWith(add)) {
            return add + str;
        }

        return str;
    }

}
