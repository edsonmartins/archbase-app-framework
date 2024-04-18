package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.validation.message.ArchbaseMessageInterpolator;
import org.hibernate.validator.HibernateValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Configuration
public class ArchbaseBeanValidateConfiguration {


    @Bean(name = "validator")
    @ConditionalOnMissingBean(Validator.class)
    public Validator getCurrentValidate() {
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().messageInterpolator(
                new ArchbaseMessageInterpolator()
        ).buildValidatorFactory();

        return validatorFactory.usingContext()
                .messageInterpolator(new ArchbaseMessageInterpolator())
                .getValidator();
    }

}
