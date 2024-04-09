package br.com.archbase.shared.kernel.converters;

import br.com.archbase.shared.kernel.quantity.Quantity;
import br.com.archbase.shared.kernel.quantity.QuantityFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Locale;

/**
 * Uma implementação do {@link Converter} que é registrada para conversão de propriedade do aplicativo (via
 * {@link ConfigurationPropertiesBinding}).
 */
@Component
@RequiredArgsConstructor
@ConfigurationPropertiesBinding
class StringToQuantityConverter implements Converter<String, Quantity> {

    @Autowired
    private QuantityFormatter formatter;

    /*
     * (non-Javadoc)
     * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
     */
    @Override
    public Quantity convert(String source) {
        try {
            return formatter.parse(source, Locale.getDefault());
        } catch (ParseException ex) {
            var sourceDescriptor = TypeDescriptor.valueOf(String.class);
            var targetDescriptor = TypeDescriptor.valueOf(Quantity.class);
            throw new ConversionFailedException(sourceDescriptor, targetDescriptor, source, ex);
        }
    }
}
