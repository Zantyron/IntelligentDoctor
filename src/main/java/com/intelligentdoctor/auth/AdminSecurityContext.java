package com.intelligentdoctor.auth;

public final class AdminSecurityContext {

    private static final ThreadLocal<AdminPrincipal> CURRENT = new ThreadLocal<>();

    private AdminSecurityContext() {
    }

    public static void set(AdminPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AdminPrincipal get() {
        return CURRENT.get();
    }

    public static AdminPrincipal require() {
        AdminPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new IllegalStateException("admin principal is not authenticated");
        }
        return principal;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
