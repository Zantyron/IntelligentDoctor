package com.intelligentdoctor.tenant;

public record TenantDefinition(
        String hospitalId,
        String domain,
        String mysqlUrl,
        String mongoUri,
        String redisPrefix,
        String pineconeNamespace,
        boolean enabled
) {
}
