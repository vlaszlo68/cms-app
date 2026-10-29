package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Optional;

/** Persistence contract for Site-owned media metadata. */
public interface MediaDao extends CrudDao<Media, MediaProperty> {

    /**
     * Counts media metadata directly owned by a Site.
     *
     * @param site owning persisted Site
     * @return number of media records owned by the Site
     */
    long countBySite(Site site);

    /**
     * Lists all media metadata owned by one Site.
     *
     * @param site owning Site with a persisted id
     * @return Site-owned media metadata
     */
    List<Media> findAllBySite(Site site);

    /**
     * Lists active media metadata owned by one Site.
     *
     * @param site owning Site with a persisted id
     * @return active Site-owned media metadata
     */
    List<Media> findActiveBySite(Site site);

    /**
     * Finds a media id only within one Site.
     *
     * @param site owning Site with a persisted id
     * @param id media id
     * @return matching Site-owned media metadata, if present
     */
    Optional<Media> findBySiteAndId(Site site, Long id);
}
