package hu.laci.cms.backend.service;

import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.user.User;
import org.mindrot.jbcrypt.BCrypt;

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

    /**
     * Creates the service with the required user DAO.
     *
     * @param userDao DAO used to load users by login name
     */
    public AuthService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
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
        try {
            Optional<User> userOptional = userDao.findByLoginName(loginName);
            if (userOptional.isEmpty()) {
                return Optional.empty();
            }

            User user = userOptional.get();
            if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                return Optional.empty();
            }

            return Optional.of(user);
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthServiceException("Failed to authenticate user: " + loginName, e);
        }
    }
}
