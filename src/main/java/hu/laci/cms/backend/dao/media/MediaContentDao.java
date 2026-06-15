package hu.laci.cms.backend.dao.media;

public interface MediaContentDao {

    void saveContent(Long mediaId, byte[] content);

    byte[] loadContent(Long mediaId);

    void deleteContent(Long mediaId);
}
