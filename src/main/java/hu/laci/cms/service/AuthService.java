package hu.laci.cms.service;

import hu.laci.cms.dao.DataAccessException;
import hu.laci.cms.dao.UserDao;
import hu.laci.cms.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

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
