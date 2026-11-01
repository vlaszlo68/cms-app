package hu.laci.cms.backend.dao.content;
import hu.laci.cms.backend.dao.common.CrudDao; import hu.laci.cms.backend.model.content.*; import hu.laci.cms.backend.model.site.Site; import java.util.*;
/** Persistence contract for Site-owned tags. */ public interface TagDao extends CrudDao<Tag,TagProperty>{ List<Tag> findBySite(Site s); Optional<Tag> findBySiteAndId(Site s,Long id); Optional<Tag> findBySiteAndCode(Site s,String code); long countContentItems(Tag tag); void deleteUnusedBySite(Site site); }
