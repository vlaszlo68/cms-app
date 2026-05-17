package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;

import java.util.Optional;

public class UserDaoImpl extends BaseDao<User, UserProperty> implements UserDao {

    public UserDaoImpl() {
        super(User.class);
    }

    @Override
    public Optional<User> findByLoginName(String loginName) {
        return findOneByProperty("loginName", loginName, "Failed to find user by login name: " + loginName);
    }

    @Override
    public Optional<User> findByEmail(String emailAddress) {
        return findOneByProperty("emailAddress", emailAddress, "Failed to find user by email: " + emailAddress);
    }
}
