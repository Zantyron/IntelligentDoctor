package com.intelligentdoctor.admin.service;

import com.intelligentdoctor.admin.dto.AdminUserView;
import com.intelligentdoctor.admin.dto.CreateAdminUserRequest;
import com.intelligentdoctor.admin.dto.ResetAdminPasswordRequest;
import com.intelligentdoctor.admin.dto.UpdateAdminUserRequest;
import com.intelligentdoctor.admin.entity.AdminUserEntity;
import com.intelligentdoctor.admin.repository.AdminUserRepository;
import com.intelligentdoctor.auth.AdminPrincipal;
import com.intelligentdoctor.auth.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserManagementService {

    private static final String TERMINAL_ROLE = "TERMINAL_USER";

    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;

    public AdminUserManagementService(AdminUserRepository adminUserRepository,
                                      PasswordService passwordService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordService = passwordService;
    }

    public List<AdminUserView> listUsers(AdminPrincipal principal) {
        return adminUserRepository.findByHospitalIdAndRoleOrderByCreatedAtDesc(principal.hospitalId(), TERMINAL_ROLE).stream()
                .map(user -> toView(user, principal))
                .toList();
    }

    @Transactional
    public AdminUserView createUser(AdminPrincipal principal, CreateAdminUserRequest request) {
        String username = normalizeUsername(request.username());
        if (adminUserRepository.existsByHospitalIdAndUsernameAndRole(principal.hospitalId(), username, TERMINAL_ROLE)) {
            throw new IllegalArgumentException("导诊终端账号已存在: " + username);
        }
        AdminUserEntity user = new AdminUserEntity();
        user.setHospitalId(principal.hospitalId());
        user.setUsername(username);
        user.setRole(TERMINAL_ROLE);
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
        return toView(adminUserRepository.save(user), principal);
    }

    @Transactional
    public AdminUserView updateUser(AdminPrincipal principal, String userId, UpdateAdminUserRequest request) {
        AdminUserEntity user = findUser(principal.hospitalId(), userId);
        if (Boolean.FALSE.equals(request.enabled())) {
            ensureCanDisable(principal, user);
        }
        user.setEnabled(request.enabled());
        return toView(adminUserRepository.save(user), principal);
    }

    @Transactional
    public AdminUserView resetPassword(AdminPrincipal principal, String userId, ResetAdminPasswordRequest request) {
        AdminUserEntity user = findUser(principal.hospitalId(), userId);
        user.setPasswordHash(passwordService.hash(request.password()));
        return toView(adminUserRepository.save(user), principal);
    }

    @Transactional
    public void deleteUser(AdminPrincipal principal, String userId) {
        AdminUserEntity user = findUser(principal.hospitalId(), userId);
        ensureCanDelete(principal, user);
        adminUserRepository.delete(user);
    }

    private AdminUserEntity findUser(String hospitalId, String userId) {
        AdminUserEntity user = adminUserRepository.findByHospitalIdAndId(hospitalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("导诊终端账号不存在"));
        if (!TERMINAL_ROLE.equals(user.getRole())) {
            throw new IllegalArgumentException("只能管理导诊终端账号");
        }
        return user;
    }

    private void ensureCanDisable(AdminPrincipal principal, AdminUserEntity user) {
        if (isCurrentUser(principal, user)) {
            throw new IllegalArgumentException("不能禁用当前登录账号");
        }
        if (Boolean.TRUE.equals(user.getEnabled())
                && adminUserRepository.countByHospitalIdAndRoleAndEnabledTrue(user.getHospitalId(), TERMINAL_ROLE) <= 1) {
            throw new IllegalArgumentException("至少需要保留一个可用导诊终端账号");
        }
    }

    private void ensureCanDelete(AdminPrincipal principal, AdminUserEntity user) {
        if (isCurrentUser(principal, user)) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }
        if (Boolean.TRUE.equals(user.getEnabled())
                && adminUserRepository.countByHospitalIdAndRoleAndEnabledTrue(user.getHospitalId(), TERMINAL_ROLE) <= 1) {
            throw new IllegalArgumentException("至少需要保留一个可用导诊终端账号");
        }
    }

    private AdminUserView toView(AdminUserEntity user, AdminPrincipal principal) {
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getEnabled(),
                isCurrentUser(principal, user),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private boolean isCurrentUser(AdminPrincipal principal, AdminUserEntity user) {
        return principal.username().equals(user.getUsername());
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
