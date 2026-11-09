package hu.laci.cms.backend.model.content;
import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.site.Site;
/** Site-owned saved query definition; it stores no item memberships. */
@DbTable("content_collections") public class ContentCollection extends AuditableEntity {
 @DbColumn("site_id") private Site site; @DbColumn("code") private String code; @DbColumn("name") private String name; @DbColumn("content_type") private ContentType contentType; @DbColumn("group_code") private String groupCode; @DbColumn("sort") private ContentSort sort; @DbColumn("page_size") private Integer pageSize; @DbColumn("active") private Boolean active;
 public ContentCollection(){} public Site getSite(){return site;} public void setSite(Site v){site=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public ContentType getContentType(){return contentType;} public void setContentType(ContentType v){contentType=v;} public String getGroupCode(){return groupCode;} public void setGroupCode(String v){groupCode=v;} public ContentSort getSort(){return sort;} public void setSort(ContentSort v){sort=v;} public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;} public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
}
