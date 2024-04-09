package br.com.archbase.validation.message;

import br.com.archbase.shared.kernel.annotations.Label;
import br.com.archbase.shared.kernel.utils.ReflectionUtils;
import br.com.archbase.validation.message.exception.ArchbaseMessageInterpolatorException;
import org.hibernate.validator.internal.engine.MessageInterpolatorContext;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A interpolação de mensagens é o processo de criação de mensagens de erro para restrições violadas na validação de Bean.
 * Este interpolator foi criado para adicionar o nome do campo as mensagens padrões de bean validation usando Hibernate
 * validator.
 */
public class ArchbaseMessageInterpolator extends ResourceBundleMessageInterpolator {

    private static final Pattern MESSAGE_PARAMETER_PATTERN = Pattern.compile("(\\{[^\\}]+?\\})");

    @Override
    public String interpolate(String message, Context context) {
        String result = super.interpolate(message, context);
        if (context instanceof MessageInterpolatorContext) {
            MessageInterpolatorContext ctx = (MessageInterpolatorContext) context;
            Field reflectionData = ReflectionUtils.getFieldByName(ctx.getRootBeanType().getClass(), "reflectionData");
            if (reflectionData != null) {
                processLabelAnnotation(result, ctx, reflectionData);
            }

        }
        return result;
    }

    private void processLabelAnnotation(String result, MessageInterpolatorContext ctx, Field reflectionData) {
        try {
            SoftReference<?> ref = (SoftReference<?>) reflectionData.get(ctx.getRootBeanType());
            if (ref.isEnqueued()) {
                Object refValue = ref.get();
                Field fields = ReflectionUtils.getFieldByName(refValue.getClass(), "declaredFields");
                Stream<Field> stream = Arrays.stream((Field[]) fields.get(refValue));
                stream.forEach(item -> {
                    if (item.isAnnotationPresent(Label.class)) {
                        Label lbl = item.getAnnotation(Label.class);
                        replacePropertyNameWithPropertyValues(result, lbl.value());
                    }
                });
            }
        } catch (Exception e) {
            throw new ArchbaseMessageInterpolatorException("Não foi possível aplicar os valor no Label do campo " + reflectionData.getName());
        }
    }

    private String replacePropertyNameWithPropertyValues(String resolvedMessage, String label) {
        Matcher matcher = MESSAGE_PARAMETER_PATTERN.matcher(resolvedMessage);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String parameter = matcher.group(1);

            String propertyName = parameter.replace("{", "");
            propertyName = propertyName.replace("}", "");

            if (propertyName.equals("label")) {
                matcher.appendReplacement(sb, label);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}