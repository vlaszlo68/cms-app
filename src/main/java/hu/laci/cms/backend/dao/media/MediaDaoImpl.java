package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;

import java.util.List;

public class MediaDaoImpl extends BaseDao<Media, MediaProperty> implements MediaDao {

    public MediaDaoImpl() {
        super(Media.class);
    }

    @Override
    public List<Media> findActive() {
        return findAll(QuerySpec.<MediaProperty>create()
                .where(MediaProperty.ACTIVE).equalsTo(Boolean.TRUE)
                .orderBy(MediaProperty.CREATED_AT.desc()));
    }
}
