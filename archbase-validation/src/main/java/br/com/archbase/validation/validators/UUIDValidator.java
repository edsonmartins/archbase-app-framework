package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.UUID;
import br.com.archbase.validation.constraints.UUID.UUIDPattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class UUIDValidator implements ConstraintValidator<UUID, String> {
    // O padrão 1 foi encontrado em um aplicativo real
    public static final Pattern UUID_PATTERN_1 = Pattern
            .compile("(:?[a-f0-9]){8,8}-(:?[a-f0-9]){4,4}-(:?[a-f0-9]){4,4}-(:?[a-f0-9]){4,4}-(:?[a-f0-9]){12,12}");
    // Os padrões 2 a 4 são etapas de ajuste (espera-se que fiquem mais rápidos a cada etapa)
    public static final Pattern UUID_PATTERN_2 = Pattern
            .compile("(:?[a-f0-9]){8}-(:?[a-f0-9]){4}-(:?[a-f0-9]){4}-(:?[a-f0-9]){4}-(:?[a-f0-9]){12}");
    public static final Pattern UUID_PATTERN_3 = Pattern
            .compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    public static final Pattern UUID_PATTERN_4 = Pattern
            .compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    // O padrão 5 leva em consideração que as letras maiúsculas são válidas para
    // UUID.fromString()
    public static final Pattern UUID_PATTERN_5 = Pattern
            .compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", Pattern.CASE_INSENSITIVE);
    // O padrão 6 é retirado de http://stackoverflow.com/a/13653180 e é mais rigoroso
    public static final Pattern UUID_PATTERN_6 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
    protected Pattern pattern;

    @Override
    public void initialize(UUID annotation) {
        if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_1)) {
            pattern = UUID_PATTERN_1;
        } else if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_2)) {
            pattern = UUID_PATTERN_2;
        } else if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_3)) {
            pattern = UUID_PATTERN_3;
        } else if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_4)) {
            pattern = UUID_PATTERN_4;
        } else if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_5)) {
            pattern = UUID_PATTERN_5;
        } else if (annotation.pattern().equals(UUIDPattern.UUID_PATTERN_6)) {
            pattern = UUID_PATTERN_6;
        }
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || pattern.matcher(value).matches();
    }
}
