package hu.laci.cms.backend.service.user;

import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.user.CreateUserRequest;
import hu.laci.cms.backend.dto.user.UpdateUserRequest;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.security.PasswordPolicyValidator;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business service for user management.
 * <p>
 * It owns user CRUD validation, duplicate checks, password hashing, soft
 * deactivation, and mapping persistence entities to API response DTOs.
 */
public class UserService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String DUPLICATE_LOGIN_NAME = "DUPLICATE_LOGIN_NAME";
    public static final String DUPLICATE_EMAIL_ADDRESS = "DUPLICATE_EMAIL_ADDRESS";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao userDao;
    private final PasswordPolicyValidator passwordPolicyValidator;

    /**
     * Creates the service with the required DAO dependency.
     *
     * @param userDao user DAO
     */
    public UserService(UserDao userDao) {
        this(userDao, new PasswordPolicyValidator(SecurityConfig.getCurrent().getPasswordPolicy()));
    }

    /**
     * Creates the service with DAO and password validator dependencies.
     *
     * @param userDao user DAO
     * @param passwordPolicyValidator password policy validator
     */
    public UserService(UserDao userDao, PasswordPolicyValidator passwordPolicyValidator) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.passwordPolicyValidator = Objects.requireNonNull(passwordPolicyValidator,
                "passwordPolicyValidator must not be null");
    }

    /**
     * Lists users ordered by login name.
     *
     * @return user response list
     */
    public List<UserResponse> findAll() {
        return userDao.findAll(QuerySpec.<UserProperty>create().orderBy(UserProperty.LOGIN_NAME.asc()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Loads one user by id.
     *
     * @param id user id
     * @return user response
     * @throws UserServiceException when no user exists
     */
    public UserResponse findById(Long id) {
        return toResponse(loadUser(id));
    }

    /**
     * Creates a new user.
     *
     * @param request create request
     * @return created user response
     */
    public UserResponse create(CreateUserRequest request) {
        validateCreateRequest(request);

        String loginName = trim(request.getLoginName());
        String emailAddress = trim(request.getEmailAddress());
        ensureLoginNameAvailable(loginName, null);
        ensureEmailAddressAvailable(emailAddress, null);

        User user = new User(
                null,
                trim(request.getUserName()),
                loginName,
                emailAddress,
                BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()),
                request.getRole(),
                request.getActive() == null ? Boolean.TRUE : request.getActive(),
                request.getRegistrationStatus() == null ? RegistrationState.PENDING : request.getRegistrationStatus()
        );

        User createdUser = userDao.create(user);
        LOGGER.info("Created user id={}, loginName={}", createdUser.getId(), createdUser.getLoginName());
        return toResponse(createdUser);
    }

    /**
     * Updates an existing user.
     *
     * @param id user id
     * @param request update request
     * @return updated user response
     */
    public UserResponse update(Long id, UpdateUserRequest request) {
        validateUpdateRequest(request);

        User user = loadUser(id);
        String loginName = trim(request.getLoginName());
        String emailAddress = trim(request.getEmailAddress());
        ensureLoginNameAvailable(loginName, id);
        ensureEmailAddressAvailable(emailAddress, id);

        user.setUserName(trim(request.getUserName()));
        user.setLoginName(loginName);
        user.setEmailAddress(emailAddress);
        user.setRole(request.getRole());
        user.setActive(request.getActive());
        user.setRegistrationState(request.getRegistrationStatus() == null
                ? user.getRegistrationState()
                : request.getRegistrationStatus());

        if (!isBlank(request.getPassword())) {
            validatePasswordPolicy(request.getPassword());
            user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        }

        User updatedUser = userDao.update(user);
        LOGGER.info("Updated user id={}, loginName={}", updatedUser.getId(), updatedUser.getLoginName());
        return toResponse(updatedUser);
    }

    /**
     * Soft-deactivates a user by setting {@code active=false}.
     *
     * @param id user id
     * @return deactivated user response
     */
    public UserResponse deactivate(Long id) {
        User user = loadUser(id);
        user.setActive(Boolean.FALSE);
        User updatedUser = userDao.update(user);
        LOGGER.info("Deactivated user id={}, loginName={}", updatedUser.getId(), updatedUser.getLoginName());
        return toResponse(updatedUser);
    }

    /**
     * Approves a pending registration and activates the user.
     *
     * @param id user id
     * @return approved user response
     */
    public UserResponse approve(Long id) {
        User user = loadUser(id);
        user.setRegistrationState(RegistrationState.COMPLETED);
        user.setActive(Boolean.TRUE);
        User updatedUser = userDao.update(user);
        LOGGER.info("Approved user id={}, loginName={}", updatedUser.getId(), updatedUser.getLoginName());
        return toResponse(updatedUser);
    }

    /**
     * Rejects a registration and keeps the user inactive.
     *
     * @param id user id
     * @return rejected user response
     */
    public UserResponse reject(Long id) {
        User user = loadUser(id);
        user.setRegistrationState(RegistrationState.REJECTED);
        user.setActive(Boolean.FALSE);
        User updatedUser = userDao.update(user);
        LOGGER.info("Rejected user id={}, loginName={}", updatedUser.getId(), updatedUser.getLoginName());
        return toResponse(updatedUser);
    }

    private User loadUser(Long id) {
        if (id == null) {
            throw new UserServiceException(VALIDATION_ERROR, "User id is required.");
        }

        return userDao.findById(id)
                .orElseThrow(() -> new UserServiceException(USER_NOT_FOUND, "User not found."));
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (request == null) {
            throw new UserServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        validateCommonFields(request.getLoginName(), request.getUserName(), request.getEmailAddress(),
                request.getRole(), request.getActive());
        if (isBlank(request.getPassword())) {
            throw new UserServiceException(VALIDATION_ERROR, "password is required.");
        }
        validatePasswordPolicy(request.getPassword());
    }

    private void validateUpdateRequest(UpdateUserRequest request) {
        if (request == null) {
            throw new UserServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        validateCommonFields(request.getLoginName(), request.getUserName(), request.getEmailAddress(),
                request.getRole(), request.getActive());
    }

    private void validateCommonFields(String loginName, String userName, String emailAddress, UserRole role,
                                      Boolean active) {
        if (isBlank(loginName)) {
            throw new UserServiceException(VALIDATION_ERROR, "loginName is required.");
        }
        if (isBlank(userName)) {
            throw new UserServiceException(VALIDATION_ERROR, "userName is required.");
        }
        if (isBlank(emailAddress)) {
            throw new UserServiceException(VALIDATION_ERROR, "emailAddress is required.");
        }
        if (!EMAIL_PATTERN.matcher(trim(emailAddress)).matches()) {
            throw new UserServiceException(VALIDATION_ERROR, "emailAddress must be a valid email address.");
        }
        if (role == null) {
            throw new UserServiceException(VALIDATION_ERROR, "role is required.");
        }
        if (active == null) {
            throw new UserServiceException(VALIDATION_ERROR, "active is required.");
        }
    }

    private void ensureLoginNameAvailable(String loginName, Long currentUserId) {
        Optional<User> existingUser = userDao.findByLoginName(loginName);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId)) {
            throw new UserServiceException(DUPLICATE_LOGIN_NAME, "loginName is already used.");
        }
    }

    private void ensureEmailAddressAvailable(String emailAddress, Long currentUserId) {
        Optional<User> existingUser = userDao.findByEmail(emailAddress);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId)) {
            throw new UserServiceException(DUPLICATE_EMAIL_ADDRESS, "emailAddress is already used.");
        }
    }

    private void validatePasswordPolicy(String password) {
        List<String> errors = passwordPolicyValidator.validate(password);
        if (!errors.isEmpty()) {
            throw new UserServiceException(VALIDATION_ERROR,
                    "Password policy validation failed.", errors);
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
