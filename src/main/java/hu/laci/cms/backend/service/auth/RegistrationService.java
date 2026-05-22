package hu.laci.cms.backend.service.auth;

import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.RegisterRequest;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.security.PasswordPolicyValidator;
import hu.laci.cms.backend.service.user.UserService;
import hu.laci.cms.backend.service.user.UserServiceException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business service for public user registration.
 * <p>
 * It validates public input, enforces duplicate checks and password policy,
 * and creates inactive USER accounts awaiting administrator approval.
 */
public class RegistrationService {

    public static final String CAPTCHA_INVALID = "CAPTCHA_INVALID";

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao userDao;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final CaptchaService captchaService;

    public RegistrationService(UserDao userDao, PasswordPolicyValidator passwordPolicyValidator,
                               CaptchaService captchaService) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.passwordPolicyValidator = Objects.requireNonNull(passwordPolicyValidator,
                "passwordPolicyValidator must not be null");
        this.captchaService = Objects.requireNonNull(captchaService, "captchaService must not be null");
    }

    public UserResponse register(RegisterRequest request, String expectedCaptchaId, Integer expectedCaptchaAnswer) {
        validateRequest(request);
        if (!captchaService.validate(expectedCaptchaId, expectedCaptchaAnswer,
                request.getCaptchaId(), request.getCaptchaAnswer())) {
            throw new UserServiceException(CAPTCHA_INVALID, "Captcha validation failed.");
        }

        String loginName = trim(request.getLoginName());
        String emailAddress = trim(request.getEmailAddress());
        ensureLoginNameAvailable(loginName);
        ensureEmailAddressAvailable(emailAddress);
        validatePasswordPolicy(request.getPassword());

        User user = new User(
                null,
                trim(request.getUserName()),
                loginName,
                emailAddress,
                BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()),
                UserRole.USER,
                Boolean.FALSE,
                RegistrationState.PENDING
        );

        User createdUser = userDao.create(user);
        LOGGER.info("Registered pending user id={}, loginName={}", createdUser.getId(), createdUser.getLoginName());
        return toResponse(createdUser);
    }

    private void validateRequest(RegisterRequest request) {
        if (request == null) {
            throw new UserServiceException(UserService.VALIDATION_ERROR, "Request body is required.");
        }
        if (isBlank(request.getLoginName())) {
            throw new UserServiceException(UserService.VALIDATION_ERROR, "loginName is required.");
        }
        if (isBlank(request.getUserName())) {
            throw new UserServiceException(UserService.VALIDATION_ERROR, "userName is required.");
        }
        if (isBlank(request.getEmailAddress())) {
            throw new UserServiceException(UserService.VALIDATION_ERROR, "emailAddress is required.");
        }
        if (!EMAIL_PATTERN.matcher(trim(request.getEmailAddress())).matches()) {
            throw new UserServiceException(UserService.VALIDATION_ERROR,
                    "emailAddress must be a valid email address.");
        }
        if (isBlank(request.getPassword())) {
            throw new UserServiceException(UserService.VALIDATION_ERROR, "password is required.");
        }
    }

    private void validatePasswordPolicy(String password) {
        List<String> errors = passwordPolicyValidator.validate(password);
        if (!errors.isEmpty()) {
            throw new UserServiceException(UserService.VALIDATION_ERROR,
                    "Password policy validation failed.", errors);
        }
    }

    private void ensureLoginNameAvailable(String loginName) {
        Optional<User> existingUser = userDao.findByLoginName(loginName);
        if (existingUser.isPresent()) {
            throw new UserServiceException(UserService.DUPLICATE_LOGIN_NAME, "loginName is already used.");
        }
    }

    private void ensureEmailAddressAvailable(String emailAddress) {
        Optional<User> existingUser = userDao.findByEmail(emailAddress);
        if (existingUser.isPresent()) {
            throw new UserServiceException(UserService.DUPLICATE_EMAIL_ADDRESS, "emailAddress is already used.");
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getLoginName(),
                user.getUserName(),
                user.getEmailAddress(),
                user.getRole(),
                user.getActive(),
                user.getRegistrationState(),
                toIsoString(user.getCreatedAt()),
                toIsoString(user.getUpdatedAt())
        );
    }

    private static String toIsoString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
