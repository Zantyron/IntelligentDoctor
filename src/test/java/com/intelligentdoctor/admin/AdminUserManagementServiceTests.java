package com.intelligentdoctor.admin;

import com.intelligentdoctor.admin.dto.CreateAdminUserRequest;
import com.intelligentdoctor.admin.dto.UpdateAdminUserRequest;
import com.intelligentdoctor.admin.entity.AdminUserEntity;
import com.intelligentdoctor.admin.repository.AdminUserRepository;
import com.intelligentdoctor.admin.service.AdminUserManagementService;
import com.intelligentdoctor.auth.AdminPrincipal;
import com.intelligentdoctor.auth.PasswordService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserManagementServiceTests {

    private static final AdminPrincipal PRINCIPAL =
            new AdminPrincipal("hospital-demo", "admin", "HOSPITAL_ADMIN");

    @Test
    void createUserNormalizesMachineAccountAndHashesPassword() {
        AdminUserRepository repository = mock(AdminUserRepository.class);
        PasswordService passwordService = new PasswordService();
        AdminUserManagementService service = new AdminUserManagementService(repository, passwordService);
        when(repository.existsByHospitalIdAndUsernameAndRole("hospital-demo", "machine-01", "TERMINAL_USER"))
                .thenReturn(false);
        when(repository.save(any(AdminUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(PRINCIPAL, new CreateAdminUserRequest(" Machine-01 ", "strong-pass-01", true));

        ArgumentCaptor<AdminUserEntity> userCaptor = ArgumentCaptor.forClass(AdminUserEntity.class);
        verify(repository).save(userCaptor.capture());
        AdminUserEntity saved = userCaptor.getValue();
        assertThat(saved.getHospitalId()).isEqualTo("hospital-demo");
        assertThat(saved.getUsername()).isEqualTo("machine-01");
        assertThat(saved.getRole()).isEqualTo("TERMINAL_USER");
        assertThat(passwordService.matches("strong-pass-01", saved.getPasswordHash())).isTrue();
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    void updateUserRejectsDisablingLastEnabledTerminalAccount() {
        AdminUserRepository repository = mock(AdminUserRepository.class);
        AdminUserManagementService service = new AdminUserManagementService(repository, new PasswordService());
        AdminUserEntity terminal = new AdminUserEntity();
        terminal.setHospitalId("hospital-demo");
        terminal.setUsername("machine-01");
        terminal.setRole("TERMINAL_USER");
        terminal.setEnabled(true);
        when(repository.findByHospitalIdAndId("hospital-demo", "terminal-id")).thenReturn(Optional.of(terminal));
        when(repository.countByHospitalIdAndRoleAndEnabledTrue("hospital-demo", "TERMINAL_USER")).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUser(PRINCIPAL, "terminal-id", new UpdateAdminUserRequest(false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少需要保留一个可用导诊终端账号");

        verify(repository, never()).save(any(AdminUserEntity.class));
    }
}
