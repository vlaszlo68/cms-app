package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Optional;

public class MediaDaoImpl extends BaseDao<Media, MediaProperty> implements MediaDao {

    public MediaDaoImpl() {
        super(Media.class);
    }

    @Override
    public long countBySite(Site site) {
        return findCustomOne("countMediaBySite", "SELECT COUNT(*) AS row_count FROM media WHERE site_id = ?",
                List.of(site), resultSet -> resultSet.getLong("row_count"), "Failed to count Site media.").orElse(0L);
    }

    @Override
    public List<Media> findAllBySite(Site site) {
        return findAll(QuerySpec.<MediaProperty>create()
                .where(MediaProperty.SITE).equalsTo(site)
                .orderBy(MediaProperty.CREATED_AT.desc()));
    }

    @Override
    public List<Media> findActiveBySite(Site site) {
        return findAll(QuerySpec.<MediaProperty>create()
                .where(MediaProperty.SITE).equalsTo(site)
                .where(MediaProperty.ACTIVE).equalsTo(Boolean.TRUE)
                .orderBy(MediaProperty.CREATED_AT.desc()));
    }

    @Override
    public Optional<Media> findBySiteAndId(Site site, Long id) {
        return findAll(QuerySpec.<MediaProperty>create()
                .where(MediaProperty.SITE).equalsTo(site)
                .where(MediaProperty.ID).equalsTo(id))
                .stream().findFirst();
    }
}
