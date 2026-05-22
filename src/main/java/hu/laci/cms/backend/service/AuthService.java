package hu.laci.cms.backend.service;

import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.security.InMemoryRateLimiter;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Authentication service that validates user credentials.
 * <p>
 * The service keeps authentication business logic outside the servlet layer:
 * it loads the user by login name through {@link UserDao} and verifies the
 * submitted password against the stored BCrypt hash.
 */
public class AuthService {

    private final UserDao userDao;
    private final InMemoryRateLimiter loginAttemptLimiter;

    /**
     * Creates the service with the required user DAO.
     *
     * @param userDao DAO used to load users by login name
     */
    public AuthService(UserDao userDao) {
        this(userDao, new InMemoryRateLimiter(
                SecurityConfig.getCurrent().getMaxFailedAttempts(),
                Duration.ofMinutes(SecurityConfig.getCurrent().getLockMinutes())
        ));
    }

    /**
     * Creates the service with the required user DAO and login attempt limiter.
     *
     * @param userDao DAO used to load users by login name
     * @param loginAttemptLimiter limiter used for failed login attempts
     */
    public AuthService(UserDao userDao, InMemoryRateLimiter loginAttemptLimiter) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.loginAttemptLimiter = Objects.requireNonNull(loginAttemptLimiter, "loginAttemptLimiter must not be null");
    }

    /**
     * Attempts to authenticate a user.
     * <p>
     * Invalid login name or password is represented as {@link Optional#empty()}.
     * Infrastructure failures are propagated as {@link DataAccessException} or
     * wrapped in {@link AuthServiceException}.
     *
     * @param loginName submitted login name
     * @param password submitted plain-text password
     * @return authenticated persistence user, or empty when credentials are invalid
     */
    public Optional<User> login(String loginName, String password) {
        return login(loginName, password, "");
    }

    /**
     * Attempts to authenticate a user and applies loginName + IP lockout rules.
     *
     * @param loginName submitted login name
     * @param password submitted plain-text password
     * @param ipAddress client IP address
     * @return authenticated persistence user, or empty when credentials are invalid or temporarily locked
     */
    public Optional<User> login(String loginName, String password, String ipAddress) {
        String limiterKey = limiterKey(loginName, ipAddress);
        if (loginAttemptLimiter.isLocked(limiterKey)) {
            return Optional.empty();
        }

        try {
            Optional<User> userOptional = userDao.findByLoginName(loginName);
            if (userOptional.isEmpty()) {
                loginAttemptLimiter.recordFailure(limiterKey);
                return Optional.empty();
            }

            User user = userOptional.get();
            if (!Boolean.TRUE.equals(user.getActive()) || !BCrypt.checkpw(password, user.getPasswordHash())) {
                loginAttemptLimiter.recordFailure(limiterKey);
                return Optional.empty();
            }

            loginAttemptLimiter.recordSuccess(limiterKey);
            return Optional.of(user);
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthServiceException("Failed to authenticate user: " + loginName, e);
        }
    }

    private static String limiterKey(String loginName, String ipAddress) {
        String normalizedLogin = loginName == null ? "" : loginName.trim().toLowerCase();
        String normalizedIp = ipAddress == null ? "" : ipAddress.trim();
        return normalizedLogin + "|" + normalizedIp;
    }
}
