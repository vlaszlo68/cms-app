package hu.laci.cms.backend.dao.common;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the current row of a {@link ResultSet} to an object.
 *
 * @param <T> mapped result type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current result-set row.
     * <p>
     * Implementations must not call {@link ResultSet#next()}; row iteration is
     * handled by the DAO helper method.
     * <p>
     * Example mapping a {@code User} entity:
     *
     * <pre>{@code
     * RowMapper<User> userMapper = resultSet -> {
     *     User user = new User();
     *     user.setId(resultSet.getLong("users_id"));
     *     user.setUserName(resultSet.getString("users_user_name"));
     *     user.setLoginName(resultSet.getString("users_login_name"));
     *     user.setEmailAddress(resultSet.getString("users_email_address"));
     *     user.setPasswordHash(resultSet.getString("users_password_hash"));
     *     return user;
     * };
     * }</pre>
     *
     * @param resultSet result set positioned on the current row
     * @return mapped object
     * @throws SQLException when reading a column fails
     */
    T map(ResultSet resultSet) throws SQLException;
}
