package com.intelligentdoctor.tenant;

import com.intelligentdoctor.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class TenantRegistry {

    private final AppProperties properties;

    public TenantRegistry(AppProperties properties) {
        this.properties = properties;
    }

    public TenantDefinition resolveByHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        Optional<TenantDefinition> configured = configuredTenants().stream()
                .filter(TenantDefinition::enabled)
                .filter(tenant -> host.equals(normalizeHost(tenant.domain()))
                        || host.startsWith(normalizeHost(tenant.hospitalId()) + "."))
                .findFirst();
        return configured.orElseGet(() -> defaultTenant(host));
    }

    public TenantDefinition resolveByHospitalId(String hospitalId) {
        return configuredTenants().stream()
                .filter(TenantDefinition::enabled)
                .filter(tenant -> tenant.hospitalId().equals(hospitalId))
                .findFirst()
                .orElseGet(() -> defaultTenant(hospitalId));
    }

    public String redisPrefix(String hospitalId) {
        return resolveByHospitalId(hospitalId).redisPrefix();
    }

    private List<TenantDefinition> configuredTenants() {
        return properties.getTenants().stream()
                .map(tenant -> new TenantDefinition(
                        tenant.getHospitalId(),
                        tenant.getDomain(),
                        tenant.getMysqlUrl(),
                        tenant.getMongoUri(),
                        tenant.getRedisPrefix(),
                        tenant.getPineconeNamespace(),
                        tenant.isEnabled()
                ))
                .toList();
    }

    private TenantDefinition defaultTenant(String source) {
        String hospitalId = properties.getDefaultHospitalId();
        return new TenantDefinition(
                hospitalId,
                source == null || source.isBlank() ? hospitalId : source,
                "",
                "",
                "intelligent-doctor:" + hospitalId,
                properties.getVectorStore().getNamespacePrefix() + "-" + hospitalId,
                true
        );
    }

    private String normalizeHost(String value) {
        if (value == null) {
            return "";
        }
        String host = value.trim().toLowerCase(Locale.ROOT);
        int comma = host.indexOf(',');
        if (comma >= 0) {
            host = host.substring(0, comma).trim();
        }
        int colon = host.indexOf(':');
        return colon >= 0 ? host.substring(0, colon) : host;
    }
}
