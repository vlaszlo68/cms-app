package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseDao<T> {

    protected Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    protected void setParameters(PreparedStatement preparedStatement, List<?> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            preparedStatement.setObject(index + 1, parameters.get(index));
        }
    }

    protected Optional<T> findOne(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(rowMapper.map(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected List<T> findList(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(rowMapper.map(resultSet));
                }

                return results;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
