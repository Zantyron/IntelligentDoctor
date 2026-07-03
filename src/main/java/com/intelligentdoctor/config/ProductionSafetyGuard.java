package com.intelligentdoctor.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionSafetyGuard implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_ADMIN_TOKEN_SECRET = "change-me-admin-token-secret";

    private final AppProperties properties;
    private final Environment environment;

    public ProductionSafetyGuard(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }
        AppProperties.Admin admin = properties.getAdmin();
        if (isBlank(admin.getPassword()) || DEFAULT_ADMIN_PASSWORD.equals(admin.getPassword())) {
            throw new IllegalStateException("ADMIN_PASSWORD must be set to a non-default value in prod profile");
        }
        if (isBlank(admin.getTokenSecret()) || DEFAULT_ADMIN_TOKEN_SECRET.equals(admin.getTokenSecret())
                || admin.getTokenSecret().length() < 32) {
            throw new IllegalStateException("ADMIN_TOKEN_SECRET must be at least 32 characters and non-default in prod profile");
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
