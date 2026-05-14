package hu.laci.cms.backend.dao;

import hu.laci.cms.backend.model.User;
import hu.laci.cms.backend.model.UserFilter;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    List<User> getList();

    List<User> getList(UserFilter filter);

    Optional<User> findById(Long id);

    Optional<User> findByLoginName(String loginName);

    Optional<User> findByEmail(String emailAddress);
}
