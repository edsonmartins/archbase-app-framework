package br.com.archbase.ddd.domain.contracts;

import java.util.Collection;

public interface ValidationResult {

    public boolean isValid();

    public Collection<ValidationError> getErrors();

}
