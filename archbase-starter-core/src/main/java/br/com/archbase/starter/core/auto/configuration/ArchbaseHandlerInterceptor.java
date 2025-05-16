package br.com.archbase.starter.core.auto.configuration;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface base para interceptors do Archbase.
 * Classes que implementam esta interface e são anotadas com {@link ArchbaseInterceptor}
 * serão automaticamente registradas.
 */
public interface ArchbaseHandlerInterceptor extends HandlerInterceptor {

    /**
     * Chamado antes do handler ser executado.
     * 
     * @param request a requisição HTTP atual
     * @param response a resposta HTTP atual
     * @param handler o handler que será executado
     * @return true se o handler deve ser executado, false caso contrário
     * @throws Exception em caso de erro
     */
    @Override
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return true;
    }

    /**
     * Chamado após o handler ser executado, mas antes da view ser renderizada.
     * 
     * @param request a requisição HTTP atual
     * @param response a resposta HTTP atual
     * @param handler o handler que foi executado
     * @param modelAndView o ModelAndView que será usado para renderizar a view
     * @throws Exception em caso de erro
     */
    @Override
    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Implementação padrão vazia
    }

    /**
     * Chamado após a requisição ser completada, incluindo renderização da view.
     * 
     * @param request a requisição HTTP atual
     * @param response a resposta HTTP atual
     * @param handler o handler que foi executado
     * @param ex a exceção que ocorreu durante o processamento, ou null se não ocorreu nenhuma
     * @throws Exception em caso de erro
     */
    @Override
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Implementação padrão vazia
    }
}