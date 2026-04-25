package hu.laci.cms.dao;

import hu.laci.cms.backend.config.DatabaseConfig;
import hu.laci.cms.model.User;
import hu.laci.cms.model.UserFilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final String BASE_SELECT_SQL = """
            SELECT id, username, login_name, email_address, password_hash
            FROM users
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, username, login_name, email_address, password_hash
            FROM users
            WHERE id = ?
            """;

    private static final String FIND_BY_LOGIN_NAME_SQL = """
            SELECT id, username, login_name, email_address, password_hash
            FROM users
            WHERE login_name = ?
            """;

    private static final String FIND_BY_EMAIL_SQL = """
            SELECT id, username, login_name, email_address, password_hash
            FROM users
            WHERE email_address = ?
            """;

    @Override
    public List<User> getList() {
        return getList(null);
    }

    @Override
    public List<User> getList(UserFilter filter) {
        StringBuilder sqlBuilder = new StringBuilder(BASE_SELECT_SQL);
        List<String> parameters = new ArrayList<>();

        appendFilters(sqlBuilder, parameters, filter);
        sqlBuilder.append(System.lineSeparator()).append("ORDER BY id");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString())) {

            setStringParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }

                return users;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get user list.", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            preparedStatement.setLong(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by id: " + id, e);
        }
    }

    @Override
    public Optional<User> findByLoginName(String loginName) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_LOGIN_NAME_SQL)) {

            preparedStatement.setString(1, loginName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by login name: " + loginName, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String emailAddress) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_EMAIL_SQL)) {

            preparedStatement.setString(1, emailAddress);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by email: " + emailAddress, e);
        }
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

    private void setStringParameters(PreparedStatement preparedStatement, List<String> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            preparedStatement.setString(index + 1, parameters.get(index));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
