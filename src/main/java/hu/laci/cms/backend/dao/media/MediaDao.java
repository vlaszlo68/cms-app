package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;

import java.util.List;

public interface MediaDao extends CrudDao<Media, MediaProperty> {

    List<Media> findActive();
}
