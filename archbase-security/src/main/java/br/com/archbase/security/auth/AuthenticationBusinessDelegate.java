package br.com.archbase.security.auth;

import br.com.archbase.security.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * Interface que permite aplicações customizarem o processo de autenticação.
 * Aplicações implementam esta interface para adicionar lógica de negócio específica
 * durante registro, login e outros processos de autenticação.
 * 
 * O framework Archbase fornece a infraestrutura de segurança, enquanto a aplicação
 * fornece a lógica de negócio através desta interface.
 */
public interface AuthenticationBusinessDelegate {
    
    /**
     * Cria ou atualiza dados de negócio após registro bem-sucedido.
     * Chamado após o usuário ser criado no sistema de segurança.
     * 
     * Exemplo: Criar entidade UserApp, enviar email de boas-vindas, 
     * criar perfil inicial, associar a tenant, etc.
     * 
     * @param user Usuário recém criado no sistema de segurança
     * @param registrationData Dados adicionais do registro fornecidos pela aplicação
     * @return ID do objeto de negócio criado (ex: UserApp ID)
     */
    String onUserRegistered(User user, Map<String, Object> registrationData);
    
    /**
     * Enriquece resposta de autenticação com dados específicos do negócio.
     * Permite adicionar informações contextuais à resposta padrão de autenticação.
     * 
     * @param baseResponse Resposta básica de autenticação do Archbase
     * @param context Contexto da aplicação (STORE_APP, CUSTOMER_APP, DRIVER_APP, WEB_ADMIN, etc)
     * @param request Requisição HTTP original para contexto adicional
     * @return Resposta enriquecida ou baseResponse se não houver enriquecimento
     */
    AuthenticationResponse enrichAuthenticationResponse(
        AuthenticationResponse baseResponse, 
        String context, 
        HttpServletRequest request
    );
    
    /**
     * Valida se um contexto é suportado pela aplicação.
     * 
     * @param context Contexto a ser validado
     * @return true se o contexto é suportado, false caso contrário
     */
    boolean supportsContext(String context);
    
    /**
     * Retorna lista de todos os contextos suportados pela aplicação.
     * 
     * @return Lista de contextos (ex: ["STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN"])
     */
    List<String> getSupportedContexts();
    
    /**
     * Chamado antes da autenticação para validações customizadas.
     * Pode lançar exceções para impedir o login baseado em regras de negócio.
     * 
     * Exemplos: Verificar se conta está suspensa, se motorista está ativo,
     * se loja está aprovada, etc.
     * 
     * @param email Email do usuário tentando autenticar
     * @param context Contexto da autenticação
     * @throws RuntimeException se validação falhar
     */
    default void preAuthenticate(String email, String context) {
        // Implementação opcional - por padrão não faz nada
    }
    
    /**
     * Chamado após autenticação bem-sucedida para ações adicionais.
     * 
     * Exemplos: Registrar login, atualizar último acesso, 
     * enviar notificação, sincronizar dados, etc.
     * 
     * @param user Usuário autenticado
     * @param context Contexto da autenticação
     */
    default void postAuthenticate(User user, String context) {
        // Implementação opcional - por padrão não faz nada
    }
    
    /**
     * Processa login com provedor externo (Google, Facebook, etc).
     * Permite criar ou atualizar usuário baseado em dados do provedor.
     * 
     * @param provider Nome do provedor (google, facebook, apple, etc)
     * @param providerData Dados fornecidos pelo provedor
     * @return ID do usuário de negócio criado/atualizado
     */
    default String onSocialLogin(String provider, Map<String, Object> providerData) {
        throw new UnsupportedOperationException(
            "Login social não implementado para provider: " + provider
        );
    }
    
    /**
     * Valida código de recuperação de senha customizado.
     * Permite implementar lógica própria de validação além do padrão.
     * 
     * @param email Email do usuário
     * @param code Código fornecido
     * @return true se código é válido
     */
    default boolean validatePasswordRecoveryCode(String email, String code) {
        // Por padrão, delega para o sistema padrão
        return true;
    }
    
    /**
     * Retorna contexto padrão quando nenhum é especificado.
     * 
     * @return Contexto padrão da aplicação
     */
    default String getDefaultContext() {
        return "DEFAULT";
    }
}