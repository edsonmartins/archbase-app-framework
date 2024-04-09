package br.com.archbase.error.handling;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author edsonmartins
 */
@ConfigurationProperties("archbase.error.handling")
public class ArchbaseErrorHandlingProperties {
    private boolean enabled = true;

    private ExceptionLogging exceptionLogging = ExceptionLogging.MESSAGE_ONLY;

    private Map<String, String> codes = new HashMap<>();

    private Map<String, String> messages = new HashMap<>();


    public ArchbaseErrorHandlingProperties() {
        super();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ExceptionLogging getExceptionLogging() {
        return exceptionLogging;
    }

    public void setExceptionLogging(ExceptionLogging exceptionLogging) {
        this.exceptionLogging = exceptionLogging;
    }

    public Map<String, String> getCodes() {
        return codes;
    }

    public void setCodes(Map<String, String> codes) {
        this.codes = codes;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    enum ExceptionLogging {
        NO_LOGGING,
        MESSAGE_ONLY,
        WITH_STACKTRACE
    }
}
