package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.URL;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;

/**
 * Valide se a sequência de caracteres (por exemplo, string) é um URL válido.
 */
public class URLValidator implements ConstraintValidator<URL, CharSequence> {
    private String protocol;
    private String host;
    private int port;

    @Override
    public void initialize(URL url) {
        this.protocol = url.protocol();
        this.host = url.host();
        this.port = url.port();
    }

    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.length() == 0) {
            return true;
        }

        java.net.URL url;
        try {
            url = new java.net.URL(value.toString());
        } catch (MalformedURLException e) {
            return false;
        }

        if (protocol != null && protocol.length() > 0 && !url.getProtocol().equals(protocol)) {
            return false;
        }

        if (host != null && host.length() > 0 && !url.getHost().equals(host)) {
            return false;
        }

        return (!(port != -1 && url.getPort() != port));
    }
}
