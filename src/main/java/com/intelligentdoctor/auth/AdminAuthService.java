package com.intelligentdoctor.auth;

import com.intelligentdoctor.admin.entity.AdminUserEntity;
import com.intelligentdoctor.admin.repository.AdminUserRepository;
import com.intelligentdoctor.auth.dto.AdminLoginResponse;
import com.intelligentdoctor.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;
    private final AdminTokenService tokenService;

    public AdminAuthService(AdminUserRepository adminUserRepository,
                            PasswordService passwordService,
                            AdminTokenService tokenService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    public AdminLoginResponse login(String username, String password) {
        String hospitalId = TenantContext.requireHospitalId();
        AdminUserEntity user = adminUserRepository.findByHospitalIdAndUsername(hospitalId, username)
                .orElseThrow(() -> new IllegalArgumentException("后台账号或密码不正确"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !passwordService.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("后台账号或密码不正确");
        }
        AdminPrincipal principal = new AdminPrincipal(hospitalId, user.getUsername(), "HOSPITAL_ADMIN");
        AdminTokenService.TokenIssue token = tokenService.issue(principal);
        return new AdminLoginResponse(token.token(), "Bearer", token.expiresAtEpochSeconds(),
                hospitalId, user.getUsername(), principal.role());
    }
}
