package br.com.archbase.security.mfa;

import br.com.archbase.security.crypto.ArchbaseCryptoService;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestão do segundo fator (MFA/2FA por TOTP) de um usuário: configuração, ativação,
 * desativação e verificação. O segredo TOTP é persistido <b>cifrado</b> (AES-GCM via
 * {@link ArchbaseCryptoService}) e os códigos de recuperação como <b>hash bcrypt</b>
 * (um por linha), consumidos ao uso. A geração/validação dos códigos é do {@link TotpService}.
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String RECOVERY_SEPARATOR = "\n";
    /** Alfabeto sem caracteres ambíguos (0/O, 1/I/L) para os códigos de recuperação. */
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserJpaRepository userRepository;
    private final TotpService totpService;
    private final ArchbaseCryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public boolean isMfaEnabled(UserEntity user) {
        return user != null && Boolean.TRUE.equals(user.getMfaEnabled());
    }

    /**
     * Inicia a configuração: gera um novo segredo TOTP, persiste-o cifrado (ainda
     * <b>não habilitado</b>) e devolve o segredo + a URI de provisionamento para o QR Code.
     */
    @Transactional
    public MfaSetup iniciarConfiguracao(String userId, String issuer) {
        UserEntity user = carregar(userId);
        String secret = totpService.generateSecret();
        user.setMfaSecret(cryptoService.encrypt(secret));
        user.setMfaEnabled(false);
        userRepository.save(user);
        String conta = user.getEmail() != null && !user.getEmail().isBlank()
                ? user.getEmail() : user.getUsername();
        return new MfaSetup(secret, totpService.provisioningUri(secret, conta, issuer));
    }

    /**
     * Confirma a configuração: valida o primeiro código TOTP contra o segredo armazenado;
     * se válido, habilita o MFA e gera os códigos de recuperação (retornados em claro uma
     * única vez; persistidos como hash).
     *
     * @throws IllegalStateException    se a configuração não foi iniciada
     * @throws IllegalArgumentException se o código TOTP é inválido
     */
    @Transactional
    public List<String> ativar(String userId, String codigoTotp) {
        UserEntity user = carregar(userId);
        String secret = segredoDecifrado(user);
        if (secret == null) {
            throw new IllegalStateException("Configuração de MFA não iniciada para o usuário");
        }
        if (!totpService.verifyCode(secret, codigoTotp)) {
            throw new IllegalArgumentException("Código TOTP inválido");
        }
        List<String> codigos = gerarRecoveryCodes();
        user.setMfaEnabled(true);
        user.setMfaRecoveryCodes(codigos.stream().map(passwordEncoder::encode)
                .collect(Collectors.joining(RECOVERY_SEPARATOR)));
        userRepository.save(user);
        return codigos;
    }

    /** Desativa o MFA e apaga o segredo e os códigos de recuperação. */
    @Transactional
    public void desativar(String userId) {
        UserEntity user = carregar(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);
        userRepository.save(user);
    }

    /**
     * Verifica um código no login (passo 2): aceita um TOTP válido ou um código de
     * recuperação (que é então <b>consumido</b>). Se o usuário não tem MFA, retorna true.
     */
    @Transactional
    public boolean verificar(UserEntity user, String codigo) {
        if (!isMfaEnabled(user)) {
            return true;
        }
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        String secret = segredoDecifrado(user);
        if (secret != null && totpService.verifyCode(secret, codigo.trim())) {
            return true;
        }
        return consumirRecoveryCode(user, codigo.trim());
    }

    private boolean consumirRecoveryCode(UserEntity user, String codigo) {
        String armazenados = user.getMfaRecoveryCodes();
        if (armazenados == null || armazenados.isBlank()) {
            return false;
        }
        List<String> hashes = new ArrayList<>(List.of(armazenados.split(RECOVERY_SEPARATOR)));
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(codigo, hashes.get(i))) {
                hashes.remove(i);
                user.setMfaRecoveryCodes(String.join(RECOVERY_SEPARATOR, hashes));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private String segredoDecifrado(UserEntity user) {
        return user.getMfaSecret() == null ? null : cryptoService.decrypt(user.getMfaSecret());
    }

    private List<String> gerarRecoveryCodes() {
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            codigos.add(gerarCodigo());
        }
        return codigos;
    }

    private String gerarCodigo() {
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                sb.append('-');
            }
            sb.append(RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }

    private UserEntity carregar(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
    }
}
