package hu.laci.cms.backend.config.session;

import java.util.Optional;

public final class SessionContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private SessionContext() {
    }

    public static void setCurrentUserId(Long userId) {
        if (userId == null) {
            CURRENT_USER_ID.remove();
            return;
        }

        CURRENT_USER_ID.set(userId);
    }

    public static Optional<Long> getCurrentUserId() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
