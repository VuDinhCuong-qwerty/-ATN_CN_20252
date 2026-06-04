package com.iam.app.config.context;


public final class RequestContext {
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private final String employeeCode;
    private final String username;
    private final String userId;
    private final String role;

    private RequestContext(String userId, String employeeCode, String username, String role) {
        this.userId = userId;
        this.employeeCode = employeeCode;
        this.username = username;
        this.role = role;
    }

    public static void set(String userId, String employeeCode, String username, String role) {
        HOLDER.set(new RequestContext(userId, employeeCode, username, role));
    }

    public static String getEmployeeCode() {
        RequestContext ctx = HOLDER.get();
        return ctx != null ? ctx.employeeCode : null;
    }

    public static String getUsername() {
        RequestContext ctx = HOLDER.get();
        return ctx != null ? ctx.username : null;
    }

    public static String getUserId() {
        RequestContext ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static String getRole() {
        RequestContext ctx = HOLDER.get();
        return ctx != null ? ctx.role : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

}
