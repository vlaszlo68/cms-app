package hu.laci.cms.backend.service;

import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.security.AttemptRateLimiter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mockito;

import java.util.Optional;

/** Unit tests for credential verification and login-attempt limiter behavior. */
class AuthServiceTest {

    @Test
    void successfulLoginReturnsUserAndClearsLimiterState() {
        UserDao userDao = Mockito.mock(UserDao.class);
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        User user = activeUser("Password123!");
        Mockito.when(userDao.findByLoginName("tester")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(userDao, limiter);

        Optional<User> result = service.login("tester", "Password123!", "127.0.0.1");

        Assertions.assertEquals(Optional.of(user), result);
        Mockito.verify(limiter).recordSuccess("tester|127.0.0.1");
        Mockito.verify(limiter, Mockito.never()).recordFailure(Mockito.anyString());
    }

    @Test
    void failedLoginNormalizesLimiterKeyAndRecordsFailure() {
        UserDao userDao = Mockito.mock(UserDao.class);
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        Mockito.when(userDao.findByLoginName(" Tester ")).thenReturn(Optional.empty());
        AuthService service = new AuthService(userDao, limiter);

        Optional<User> result = service.login(" Tester ", "wrong", " 127.0.0.1 ");

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(limiter).recordFailure("tester|127.0.0.1");
    }

    @Test
    void lockedLoginDoesNotQueryDao() {
        UserDao userDao = Mockito.mock(UserDao.class);
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        Mockito.when(limiter.isLocked("tester|127.0.0.1")).thenReturn(true);
        AuthService service = new AuthService(userDao, limiter);

        Optional<User> result = service.login("tester", "Password123!", "127.0.0.1");

        Assertions.assertTrue(result.isEmpty());
        Mockito.verifyNoInteractions(userDao);
    }

    @Test
    void inactiveUserIsRejectedAndCountsAsFailure() {
        UserDao userDao = Mockito.mock(UserDao.class);
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        User user = activeUser("Password123!");
        user.setActive(Boolean.FALSE);
        Mockito.when(userDao.findByLoginName("tester")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(userDao, limiter);

        Assertions.assertTrue(service.login("tester", "Password123!", "127.0.0.1").isEmpty());
        Mockito.verify(limiter).recordFailure("tester|127.0.0.1");
    }

    private User activeUser(String password) {
        return new User(1L, "Tester", "tester", "tester@example.com", BCrypt.hashpw(password, BCrypt.gensalt()),
                UserRole.ADMIN, Boolean.TRUE, RegistrationState.COMPLETED);
    }
}
