package br.com.archbase.security.service;

public interface ArchbaseEmailService {
    void sendResetPasswordEmail(String email, String resetPasswordToken, String userName, String name);
}
