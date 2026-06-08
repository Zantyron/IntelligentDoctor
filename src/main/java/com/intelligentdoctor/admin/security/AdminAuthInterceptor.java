package com.intelligentdoctor.admin.security;

import com.intelligentdoctor.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AppProperties properties;

    public AdminAuthInterceptor(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isAuthorized(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Intelligent Doctor Admin\"");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"后台账号或密码不正确\",\"data\":null}");
        return false;
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }
            String username = decoded.substring(0, separator);
            String password = decoded.substring(separator + 1);
            return properties.getAdmin().getUsername().equals(username)
                    && properties.getAdmin().getPassword().equals(password);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
