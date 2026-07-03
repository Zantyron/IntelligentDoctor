package com.intelligentdoctor.auth;

public final class TerminalSecurityContext {

    private static final ThreadLocal<AdminPrincipal> CURRENT = new ThreadLocal<>();

    private TerminalSecurityContext() {
    }

    public static void set(AdminPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AdminPrincipal get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
