package br.com.archbase.query.rls.support.exceptions;

public class ArchbaseRowLevelSecurityFilterException extends Exception {

    public ArchbaseRowLevelSecurityFilterException(String message) {
        super(message);
    }

    public ArchbaseRowLevelSecurityFilterException(String message, Throwable throwable) {
        super(message, throwable);
    }
}