package br.com.archbase.shared.kernel.quantity;

import org.springframework.format.Formatter;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Um Spring {@link Formatter} dedicado para imprimir e analisar instâncias de {@link Quantity}. Usa um
 * {@link NumberStyleFormatter} para análise do valor.
 *
 * @see NumberStyleFormatter
 */
@Component
public class QuantityFormatter implements Formatter<Quantity> {

    private static final Pattern QUANTITY_PATTERN;
    private static final NumberStyleFormatter NUMBER_FORMATTER = new NumberStyleFormatter();

    static {

        StringBuilder builder = new StringBuilder();

        builder.append("("); // grupo 1 inicio
        builder.append("[+-]?"); // sinal opcional
        builder.append("\\d*[\\.\\,]?\\d*"); // decimais com. ou, entre
        builder.append(")"); // group 1 fim

        builder.append("("); // grupo 2 inicio
        builder.append("\\s*\\w*"); // qualquer tipo de texto como fonte métrica
        builder.append(")");

        QUANTITY_PATTERN = Pattern.compile(builder.toString());
    }

    /**
     * Tenta criar uma {@link Metric} a partir da fonte fornecida e relata tentativas inválidas como {@link ParseException}
     * aplicando o mapeador de exceções fornecido.
     *
     * @param source o valor de origem que simboliza a {@link Metric}
     * @throws ParseException caso a criação de uma {@link Metric} falhe para a fonte fornecida.
     * @Retorna
     */
    private static Metric parseMetric(String source, Function<IllegalArgumentException, ParseException> exceptionMapper)
            throws ParseException {

        try {
            return StringUtils.hasText(source) ? Metric.from(source) : Metric.UNIT;
        } catch (IllegalArgumentException ex) {
            throw exceptionMapper.apply(ex);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.format.Printer#print(java.lang.Object, java.util.Locale)
     */
    @Override
    public String print(Quantity object, Locale locale) {

        return String.format("%s%s", NUMBER_FORMATTER.print(object.getAmount(), locale),
                object.getMetric().getAbbreviation());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.format.Parser#parse(java.lang.String, java.util.Locale)
     */
    @Override
    public Quantity parse(String text, Locale locale) throws ParseException {

        if (!StringUtils.hasText(text)) {
            return Quantity.of(0);
        }

        Matcher matcher = QUANTITY_PATTERN.matcher(text.trim());

        if (!matcher.matches()) {
            throw new ParseException(text, 0);
        }

        Metric metric = parseMetric(matcher.group(2), oo -> new ParseException(matcher.group(2), matcher.start(2)));

        Number number = NUMBER_FORMATTER.parse(matcher.group(1), locale);

        return number instanceof BigDecimal //
                ? Quantity.of((BigDecimal) number, metric) //
                : Quantity.of(number.doubleValue(), metric);
    }
}
