package hu.laci.cms.backend.dao.content;
import hu.laci.cms.backend.dao.common.CrudDao; import hu.laci.cms.backend.model.content.*; import hu.laci.cms.backend.model.site.Site; import java.util.*;
/** Persistence contract for saved content collection definitions. */ public interface ContentCollectionDao extends CrudDao<ContentCollection,ContentCollectionProperty>{long countBySite(Site s); List<ContentCollection> findBySite(Site s); Optional<ContentCollection> findBySiteAndId(Site s,Long id); Optional<ContentCollection> findBySiteAndCode(Site s,String code);}
