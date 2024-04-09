package br.com.archbase.error.handling;

/**
 * @author edsonmartins
 */
public class ArchbaseApiGlobalError {
    private final String code;
    private final String message;

    public ArchbaseApiGlobalError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
