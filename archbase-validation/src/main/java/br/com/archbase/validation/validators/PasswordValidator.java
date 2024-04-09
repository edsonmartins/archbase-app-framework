package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.Password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

import static java.lang.Character.*;
import static java.util.stream.Collectors.toSet;

public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final Set<Character> SPECIAL_CHARS = "\"/*!@#$%^&*()\\\"{}_[]|\\\\?/<>,.\""
            .chars()
            .mapToObj(i -> (char) i)
            .collect(toSet());

    private Password annotation;

    @Override
    public void initialize(Password constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (null == value || value.length() < annotation.minSize() || value.length() >= annotation.maxSize()) {
            return false;
        }

        int upperCase = 0;
        int lowerCase = 0;
        int specialChar = 0;
        int digits = 0;

        for (int i = 0; i < value.length(); ++i) {
            char chr = value.charAt(i);

            if (isUpperCase(chr)) upperCase++;

            else if (isLowerCase(chr)) lowerCase++;

            else if (SPECIAL_CHARS.contains(chr)) specialChar++;

            else if (isDigit(chr)) digits++;

            else if (chr == ' ' && annotation.allowSpace()) return false;
        }

        if (annotation.containsDigits() && digits <= 0) return false;
        if (annotation.containsLowerCase() && lowerCase <= 0) return false;
        if (annotation.containsUpperCase() && upperCase <= 0) return false;

        return (!(annotation.containsSpecialChar() && specialChar <= 0));

    }
}
