package br.com.archbase.resource.logger.utils;

import br.com.archbase.resource.logger.bean.RequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author edsonmartins
 */
public class RequestUtil {

    public static RequestContext getRequestContext() {
        HttpServletRequest request = getCurrentHttpRequest();

        return new RequestContext()
                .add("url", getRequestUrl(request))
                .add("username", getRequestUserName());
    }

    @Nullable
    private static String getRequestUrl(@Nullable HttpServletRequest request) {
        return request == null ? null : request.getRequestURL().toString();
    }

    @Nullable
    private static String getRequestUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Nullable
    private static HttpServletRequest getCurrentHttpRequest() {
        HttpServletRequest request = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return request;
    }

    @Nullable
    private String getRequestUserName(@Nullable UserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUsername();
    }

    @Nullable
    private UserDetails getCurrentUser() {
        UserDetails user = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = null;
        if (authentication != null) {
            principal = authentication.getPrincipal();
        }
        if (principal instanceof UserDetails) {
            user = (UserDetails) principal;
        }
        return user;
    }

}
