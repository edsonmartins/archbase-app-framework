package br.com.archbase.shared.kernel.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: holds the regexp to validate an email address<br>
 */
public class EMailValidationUtils {
    private static String ATOM = "[^\\x00-\\x1F^\\(^\\)^\\<^\\>^\\@^\\,^\\;^\\:^\\\\^\\\"^\\.^\\[^\\]^\\s]";
    private static String DOMAIN = "(" + ATOM + "+(\\." + ATOM + "+)*";
    private static String IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]";
    public static final java.util.regex.Pattern DEFAULT_EMAIL_PATTERN;

    static {
        DEFAULT_EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^" + ATOM + "+(\\." + ATOM + "+)*@" + DOMAIN + "|" + IP_DOMAIN + ")$",
                java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /**
     * Learn whether a given object is a valid email address.
     * 
     * @param value
     *            to check
     * @return <code>true</code> if the validation passes
     */
    public static boolean isValid(Object value) {
        return isValid(value, DEFAULT_EMAIL_PATTERN);
    }

    /**
     * Learn whether a particular value matches a given pattern per
     * {@link Matcher#matches()}.
     * 
     * @param value
     * @param aPattern
     * @return <code>true</code> if <code>value</code> was a <code>String</code>
     *         matching <code>aPattern</code>
     */
    // TODO it would seem to make sense to move or reduce the visibility of this
    // method as it is more general than email.
    public static boolean isValid(Object value, Pattern aPattern) {
        if (value == null)
            return true;
        if (!(value instanceof CharSequence))
            return false;
        CharSequence seq = (CharSequence) value;
        if (seq.length() == 0)
            return true;
        Matcher m = aPattern.matcher(seq);
        return m.matches();
    }

}
