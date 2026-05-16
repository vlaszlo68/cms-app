package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserFilter;
import hu.laci.cms.backend.model.user.UserSort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl extends BaseDao<User> implements UserDao {

    private static final String BASE_SELECT_SQL = """
            SELECT id, username, login_name, email_address, password_hash
            FROM users
            """;

    private static final String FIND_BY_ID_SQL = BASE_SELECT_SQL + "WHERE id = ?";

    private static final String FIND_BY_LOGIN_NAME_SQL = BASE_SELECT_SQL + "WHERE login_name = ?";

    private static final String FIND_BY_EMAIL_SQL = BASE_SELECT_SQL + "WHERE email_address = ?";

    @Override
    public List<User> getList(UserFilter filter, UserSort sort, Boolean ascending) {
        StringBuilder sqlBuilder = new StringBuilder(BASE_SELECT_SQL);
        List<String> parameters = new ArrayList<>();

        appendFilters(sqlBuilder, parameters, filter);
        appendOrder(sqlBuilder, sort, ascending);

        return findList(sqlBuilder.toString(), parameters, this::mapUser, "Failed to get user list.");
    }

    @Override
    public Optional<User> findById(Long id) {
        return findOne(FIND_BY_ID_SQL, List.of(id), this::mapUser, "Failed to find user by id: " + id);
    }

    @Override
    public Optional<User> findByLoginName(String loginName) {
        return findOne(FIND_BY_LOGIN_NAME_SQL, List.of(loginName), this::mapUser,
                "Failed to find user by login name: " + loginName);
    }

    @Override
    public Optional<User> findByEmail(String emailAddress) {
        return findOne(FIND_BY_EMAIL_SQL, List.of(emailAddress), this::mapUser,
                "Failed to find user by email: " + emailAddress);
    }

    private void appendFilters(StringBuilder sqlBuilder, List<String> parameters, UserFilter filter) {
        if (filter == null) {
            return;
        }

        List<String> conditions = new ArrayList<>();

        if (hasText(filter.getUserName())) {
            conditions.add("username LIKE ?");
            parameters.add("%" + filter.getUserName().trim() + "%");
        }

        if (hasText(filter.getLoginName())) {
            conditions.add("login_name LIKE ?");
            parameters.add("%" + filter.getLoginName().trim() + "%");
        }

        if (hasText(filter.getEmailAddress())) {
            conditions.add("email_address LIKE ?");
            parameters.add("%" + filter.getEmailAddress().trim() + "%");
        }

        if (conditions.isEmpty()) {
            return;
        }

        sqlBuilder.append(System.lineSeparator()).append("WHERE ");
        sqlBuilder.append(String.join(" AND ", conditions));
    }

    private void appendOrder(StringBuilder sqlBuilder, UserSort sort, Boolean ascending) {
        UserSort selectedSort = sort == null ? UserSort.ID : sort;
        boolean selectedAscending = ascending == null || ascending;

        sqlBuilder.append(System.lineSeparator())
                .append("ORDER BY ")
                .append(selectedSort.getColumnName())
                .append(selectedAscending ? " ASC" : " DESC");
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUserName(resultSet.getString("username"));
        user.setLoginName(resultSet.getString("login_name"));
        user.setEmailAddress(resultSet.getString("email_address"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        return user;
    }
}
