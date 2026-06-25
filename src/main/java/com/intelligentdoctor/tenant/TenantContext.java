package com.intelligentdoctor.tenant;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setHospitalId(String hospitalId) {
        CURRENT.set(hospitalId);
    }

    public static String getHospitalId() {
        return CURRENT.get();
    }

    public static String requireHospitalId() {
        String hospitalId = CURRENT.get();
        if (hospitalId == null || hospitalId.isBlank()) {
            throw new IllegalStateException("tenant hospital is not resolved");
        }
        return hospitalId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
