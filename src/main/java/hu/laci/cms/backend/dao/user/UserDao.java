package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;

import java.util.Optional;

public interface UserDao extends CrudDao<User, UserProperty> {

    Optional<User> findByLoginName(String loginName);

    Optional<User> findByEmail(String emailAddress);
}
