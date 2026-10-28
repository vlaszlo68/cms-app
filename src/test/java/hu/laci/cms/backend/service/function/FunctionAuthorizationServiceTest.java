package hu.laci.cms.backend.service.function;

import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies the central PUBLIC, authenticated, and administrator policy semantics. */
class FunctionAuthorizationServiceTest {
    private UserDao userDao;
    private FunctionAuthorizationService service;
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "user", "user@example.com", UserRole.USER);
    private final AuthenticatedUser admin = new AuthenticatedUser(2L, "admin", "admin@example.com", UserRole.ADMIN);

    @BeforeEach
    void setUp() {
        userDao = Mockito.mock(UserDao.class);
        service = new FunctionAuthorizationService(userDao);
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(activeUser(1L, UserRole.USER)));
        Mockito.when(userDao.findById(2L)).thenReturn(Optional.of(activeUser(2L, UserRole.ADMIN)));
    }

    @Test void publicAllowsAnonymous() { assertDoesNotThrow(() -> service.authorize(AccessPolicy.PUBLIC, Optional.empty())); }
    @Test void authenticatedRejectsAnonymous() { assertCode("AUTH_REQUIRED", () -> service.authorize(AccessPolicy.AUTHENTICATED, Optional.empty())); }
    @Test void authenticatedAllowsUserAndAdmin() { assertDoesNotThrow(() -> service.authorize(AccessPolicy.AUTHENTICATED, Optional.of(user))); assertDoesNotThrow(() -> service.authorize(AccessPolicy.AUTHENTICATED, Optional.of(admin))); }
    @Test void authenticatedRejectsDisabledOrDeletedSessionUser() {
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(new User(1L, "User", "user", "user@example.com", "hash", UserRole.USER, false)));
        assertCode("AUTH_REQUIRED", () -> service.authorize(AccessPolicy.AUTHENTICATED, Optional.of(user)));
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.empty());
        assertCode("AUTH_REQUIRED", () -> service.authorize(AccessPolicy.AUTHENTICATED, Optional.of(user)));
    }
    @Test void adminRejectsUserAndAllowsAdmin() { assertCode("FORBIDDEN", () -> service.authorize(AccessPolicy.ADMIN, Optional.of(user))); assertDoesNotThrow(() -> service.authorize(AccessPolicy.ADMIN, Optional.of(admin))); }
    private static User activeUser(Long id, UserRole role) { return new User(id, "User", "user", "user@example.com", "hash", role, true); }
    private void assertCode(String code, Runnable invocation) { assertEquals(code, assertThrows(PublicFunctionException.class, invocation::run).getCode()); }
}
