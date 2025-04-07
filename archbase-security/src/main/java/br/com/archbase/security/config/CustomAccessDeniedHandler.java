package br.com.archbase.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, 
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = Optional.ofNullable(auth).map(Authentication::getName).orElse("usuário não autenticado");
        
        log.error("ACESSO NEGADO: Usuário {} tentou acessar o recurso protegido: {}", 
                 username, request.getRequestURI());
        log.error("Método da requisição: {}", request.getMethod());
        log.error("Autoridades do usuário: {}", 
                 Optional.ofNullable(auth).map(Authentication::getAuthorities).orElse(null));
        log.error("Detalhes do erro de acesso: {}", accessDeniedException.getMessage(), accessDeniedException);
        
        // Verifica se há permissão necessária anotada no handler do método
        String requiredPermission = (String) request.getAttribute("requiredPermission");
        if (requiredPermission != null) {
            log.error("Permissão necessária: {}", requiredPermission);
        }
        
        // Permite que o fluxo padrão de tratamento continue
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado: " + accessDeniedException.getMessage());
    }
}