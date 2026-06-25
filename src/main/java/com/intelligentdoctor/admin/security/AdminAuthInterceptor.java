package com.intelligentdoctor.admin.security;

import com.intelligentdoctor.auth.AdminPrincipal;
import com.intelligentdoctor.auth.AdminSecurityContext;
import com.intelligentdoctor.auth.AdminTokenService;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.tenant.TenantContext;
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
    private final AdminTokenService tokenService;

    public AdminAuthInterceptor(AppProperties properties, AdminTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AdminPrincipal principal = authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (principal != null) {
            AdminSecurityContext.set(principal);
            TenantContext.setHospitalId(principal.hospitalId());
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"后台账号或密码不正确\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminSecurityContext.clear();
    }

    private AdminPrincipal authenticate(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return tokenService.verify(authorization.substring(7));
        }
        if (isLegacyBasicAuthorized(authorization)) {
            return new AdminPrincipal(TenantContext.requireHospitalId(), properties.getAdmin().getUsername(), "HOSPITAL_ADMIN");
        }
        return null;
    }

    private boolean isLegacyBasicAuthorized(String authorization) {
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
