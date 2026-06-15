package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.config.database.TransactionContext;
import hu.laci.cms.backend.dao.common.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MediaContentDaoImpl implements MediaContentDao {

    @Override
    public void saveContent(Long mediaId, byte[] content) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId must not be null.");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null.");
        }

        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement statement = connectionScope.getConnection().prepareStatement("""
                     INSERT INTO media_contents (media_id, content)
                     VALUES (?, ?)
                     ON CONFLICT (media_id) DO UPDATE SET content = EXCLUDED.content
                     """)) {
            statement.setLong(1, mediaId);
            statement.setBytes(2, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save media content for media id: " + mediaId, e);
        }
    }

    @Override
    public byte[] loadContent(Long mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId must not be null.");
        }

        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement statement = connectionScope.getConnection().prepareStatement(
                     "SELECT content FROM media_contents WHERE media_id = ?")) {
            statement.setLong(1, mediaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getBytes("content");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load media content for media id: " + mediaId, e);
        }
    }

    @Override
    public void deleteContent(Long mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId must not be null.");
        }

        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement statement = connectionScope.getConnection().prepareStatement(
                     "DELETE FROM media_contents WHERE media_id = ?")) {
            statement.setLong(1, mediaId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete media content for media id: " + mediaId, e);
        }
    }
}
