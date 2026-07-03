package com.intelligentdoctor.auth;

import com.intelligentdoctor.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TerminalAuthInterceptor implements HandlerInterceptor {

    private final AdminTokenService tokenService;

    public TerminalAuthInterceptor(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AdminPrincipal principal = authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (principal != null && ("TERMINAL_USER".equals(principal.role()) || "HOSPITAL_ADMIN".equals(principal.role()))) {
            TerminalSecurityContext.set(principal);
            TenantContext.setHospitalId(principal.hospitalId());
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"请先登录导诊终端账号\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TerminalSecurityContext.clear();
    }

    private AdminPrincipal authenticate(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return tokenService.verify(authorization.substring(7));
        }
        return null;
    }
}
