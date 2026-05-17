package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;

import java.util.Optional;

/**
 * JDBC implementation of {@link UserDao} based on {@link BaseDao}.
 */
public class UserDaoImpl extends BaseDao<User, UserProperty> implements UserDao {

    /**
     * Creates a user DAO using the {@link User} entity mapping.
     */
    public UserDaoImpl() {
        super(User.class);
    }

    /**
     * Finds one user by login name.
     *
     * @param loginName login name to search for
     * @return matching user, or empty when no user exists
     */
    @Override
    public Optional<User> findByLoginName(String loginName) {
        return findOneByProperty("loginName", loginName, "Failed to find user by login name: " + loginName);
    }

    /**
     * Finds one user by email address.
     *
     * @param emailAddress email address to search for
     * @return matching user, or empty when no user exists
     */
    @Override
    public Optional<User> findByEmail(String emailAddress) {
        return findOneByProperty("emailAddress", emailAddress, "Failed to find user by email: " + emailAddress);
    }
}
