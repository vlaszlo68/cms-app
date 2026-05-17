package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;

import java.util.Optional;

/**
 * DAO contract for {@link User} persistence and user-specific lookups.
 */
public interface UserDao extends CrudDao<User, UserProperty> {

    /**
     * Finds one user by login name.
     *
     * @param loginName login name stored in the users table
     * @return matching user, or empty when no user exists
     */
    Optional<User> findByLoginName(String loginName);

    /**
     * Finds one user by email address.
     *
     * @param emailAddress email address stored in the users table
     * @return matching user, or empty when no user exists
     */
    Optional<User> findByEmail(String emailAddress);
}
