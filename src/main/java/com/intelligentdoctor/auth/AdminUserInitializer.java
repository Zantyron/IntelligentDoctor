package com.intelligentdoctor.auth;

import com.intelligentdoctor.admin.entity.AdminUserEntity;
import com.intelligentdoctor.admin.repository.AdminUserRepository;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.tenant.TenantRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final AppProperties properties;
    private final PasswordService passwordService;
    private final TenantRegistry tenantRegistry;

    public AdminUserInitializer(AdminUserRepository adminUserRepository,
                                AppProperties properties,
                                PasswordService passwordService,
                                TenantRegistry tenantRegistry) {
        this.adminUserRepository = adminUserRepository;
        this.properties = properties;
        this.passwordService = passwordService;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public void run(String... args) {
        if (properties.getTenants().isEmpty()) {
            ensureAdmin(properties.getDefaultHospitalId());
            return;
        }
        properties.getTenants().stream()
                .filter(AppProperties.Tenant::isEnabled)
                .map(AppProperties.Tenant::getHospitalId)
                .forEach(this::ensureAdmin);
    }

    private void ensureAdmin(String hospitalId) {
        tenantRegistry.resolveByHospitalId(hospitalId);
        if (adminUserRepository.existsByHospitalIdAndUsername(hospitalId, properties.getAdmin().getUsername())) {
            return;
        }
        AdminUserEntity user = new AdminUserEntity();
        user.setHospitalId(hospitalId);
        user.setUsername(properties.getAdmin().getUsername());
        user.setPasswordHash(passwordService.hash(properties.getAdmin().getPassword()));
        user.setRole("HOSPITAL_ADMIN");
        user.setEnabled(true);
        adminUserRepository.save(user);
    }
}
