package hu.laci.cms.backend.service.auth;

import com.google.gson.Gson;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.RegisterRequest;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.security.PasswordPolicyValidator;
import hu.laci.cms.backend.service.user.UserService;
import hu.laci.cms.backend.service.user.UserServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

/** Unit tests for public registration validation and account creation rules. */
class RegistrationServiceTest {

    @Test
    void disabledCaptchaRegistrationCreatesPendingInactiveUser() {
        UserDao userDao = Mockito.mock(UserDao.class);
        PasswordPolicyValidator policy = Mockito.mock(PasswordPolicyValidator.class);
        CaptchaService captcha = Mockito.mock(CaptchaService.class);
        Mockito.when(userDao.findByLoginName("new-user")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByEmail("new@example.com")).thenReturn(Optional.empty());
        Mockito.when(policy.validate("Password123!")).thenReturn(List.of());
        Mockito.when(userDao.create(Mockito.any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(9L);
            return user;
        });
        RegistrationService service = new RegistrationService(userDao, policy, captcha);

        UserResponse response = service.register(request(), null, null, false);

        Assertions.assertEquals(9L, response.getId());
        Assertions.assertEquals(UserRole.USER, response.getRole());
        Assertions.assertFalse(response.getActive());
        Assertions.assertEquals(RegistrationState.PENDING, response.getRegistrationStatus());
        Mockito.verifyNoInteractions(captcha);
    }

    @Test
    void enabledCaptchaRejectsInvalidAnswerBeforeDaoLookup() {
        UserDao userDao = Mockito.mock(UserDao.class);
        PasswordPolicyValidator policy = Mockito.mock(PasswordPolicyValidator.class);
        CaptchaService captcha = Mockito.mock(CaptchaService.class);
        Mockito.when(captcha.validate("expected", 12, "captcha-id", "12")).thenReturn(false);
        RegistrationService service = new RegistrationService(userDao, policy, captcha);

        UserServiceException exception = Assertions.assertThrows(UserServiceException.class,
                () -> service.register(request(), "expected", 12));

        Assertions.assertEquals(RegistrationService.CAPTCHA_INVALID, exception.getCode());
        Mockito.verifyNoInteractions(userDao, policy);
    }

    @Test
    void duplicateLoginIsRejected() {
        UserDao userDao = Mockito.mock(UserDao.class);
        PasswordPolicyValidator policy = Mockito.mock(PasswordPolicyValidator.class);
        CaptchaService captcha = Mockito.mock(CaptchaService.class);
        Mockito.when(userDao.findByLoginName("new-user")).thenReturn(Optional.of(existingUser()));
        RegistrationService service = new RegistrationService(userDao, policy, captcha);

        UserServiceException exception = Assertions.assertThrows(UserServiceException.class,
                () -> service.register(request(), null, null, false));

        Assertions.assertEquals(UserService.DUPLICATE_LOGIN_NAME, exception.getCode());
        Mockito.verify(userDao, Mockito.never()).create(Mockito.any());
    }

    @Test
    void honeypotIsRejectedBeforeCaptchaAndPersistence() {
        UserDao userDao = Mockito.mock(UserDao.class);
        PasswordPolicyValidator policy = Mockito.mock(PasswordPolicyValidator.class);
        CaptchaService captcha = Mockito.mock(CaptchaService.class);
        RegisterRequest request = new Gson().fromJson("{\"loginName\":\"new-user\",\"userName\":\"New User\","
                + "\"emailAddress\":\"new@example.com\",\"password\":\"Password123!\","
                + "\"captchaHoneypot\":\"bot value\"}", RegisterRequest.class);
        RegistrationService service = new RegistrationService(userDao, policy, captcha);

        UserServiceException exception = Assertions.assertThrows(UserServiceException.class,
                () -> service.register(request, null, null, false));

        Assertions.assertEquals(UserService.VALIDATION_ERROR, exception.getCode());
        Mockito.verifyNoInteractions(userDao, policy, captcha);
    }

    private RegisterRequest request() {
        return new RegisterRequest("new-user", "New User", "new@example.com", "Password123!", "captcha-id", "12");
    }

    private User existingUser() {
        return new User(1L, "Existing", "new-user", "new@example.com", "hash", UserRole.USER,
                Boolean.FALSE, RegistrationState.PENDING);
    }
}
