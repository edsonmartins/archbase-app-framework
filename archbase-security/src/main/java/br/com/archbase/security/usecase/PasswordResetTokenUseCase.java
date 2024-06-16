package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.PasswordResetToken;
import br.com.archbase.security.persistence.UserEntity;

import java.util.List;

public interface PasswordResetTokenUseCase {
    List<PasswordResetToken> findAllNonExpiredAndNonRevokedTokens(UserEntity user);
    PasswordResetToken findToken(UserEntity user, String token);
    void saveAll(List<PasswordResetToken> passwordResetTokens);
    void save(PasswordResetToken passwordResetToken);
}
