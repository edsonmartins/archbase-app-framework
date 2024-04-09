package br.com.archbase.shared.kernel.converters;

import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;


/**
 * JPA {@link AttributeConverter} para serializar instâncias {@link MonetaryAmount} em um {@link String}. Auto-aplicado a
 * todas as propriedades da entidade do tipo {@link MonetaryAmount}.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class MonetaryAmountAttributeConverter implements AttributeConverter<MonetaryAmount, String> {

    private static final MonetaryAmountFormat FORMAT = MonetaryFormats.getAmountFormat(Locale.ROOT);

    @Override
    public String convertToDatabaseColumn(MonetaryAmount amount) {
        return amount == null ? null : amount.toString();
    }

    @Override
    public MonetaryAmount convertToEntityAttribute(String source) {
        return source == null ? null : Money.parse(source, FORMAT);
    }
}