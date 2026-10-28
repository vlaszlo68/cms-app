package hu.laci.cms.backend.service.function;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;

import java.util.Objects;
import java.util.Optional;

/**
 * Central access-policy evaluator for public application/function dispatch.
 * Protected policies re-check the persisted User state so disabling an account
 * takes effect even when the old authenticated session still exists.
 */
public class FunctionAuthorizationService {
    private final UserDao userDao;

    /**
     * Creates the evaluator with the authoritative user-state lookup.
     *
     * @param userDao DAO used only to validate principals for protected policies
     */
    public FunctionAuthorizationService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

    /**
     * Enforces an access policy using only the server-side session principal.
     *
     * @param policy trusted application or function policy
     * @param principal principal recovered from the server-side session
     * @return the permitted session principal, when supplied
     * @throws PublicFunctionException when authentication, active state, or role is insufficient
     */
    public Optional<AuthenticatedUser> authorize(AccessPolicy policy, Optional<AuthenticatedUser> principal) {
        if (policy == AccessPolicy.PUBLIC) {
            return principal;
        }
        if (principal.isEmpty() || !isActive(principal.get())) {
            throw new PublicFunctionException("AUTH_REQUIRED", "Authentication required.");
        }
        if (policy == AccessPolicy.ADMIN && principal.get().getRole() != UserRole.ADMIN) {
            throw new PublicFunctionException("FORBIDDEN", "Administrator access required.");
        }
        return principal;
    }

    private boolean isActive(AuthenticatedUser principal) {
        Optional<User> persisted = userDao.findById(principal.getId());
        return persisted.map(User::getActive).filter(Boolean.TRUE::equals).isPresent();
    }
}
