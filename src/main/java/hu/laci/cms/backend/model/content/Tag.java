package hu.laci.cms.backend.model.content;
import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.site.Site;
/** Site-owned tag with a stable code. */
@DbTable("tags") public class Tag extends AuditableEntity {
 @DbColumn("site_id") private Site site; @DbColumn("code") private String code; @DbColumn("name") private String name;
 public Tag(){} public Site getSite(){return site;} public void setSite(Site v){site=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;}
}
