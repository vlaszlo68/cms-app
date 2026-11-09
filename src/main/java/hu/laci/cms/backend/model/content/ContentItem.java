package hu.laci.cms.backend.model.content;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.site.Site;
import java.sql.Timestamp;

/** Site-owned reusable editorial content, shared by news, blog and gallery records. */
@DbTable("content_items")
public class ContentItem extends AuditableEntity {
    @DbColumn("site_id") private Site site;
    @DbColumn("type") private ContentType type;
    @DbColumn("title") private String title;
    @DbColumn("slug") private String slug;
    @DbColumn("summary") private String summary;
    @DbColumn("content") private String content;
    @DbColumn("group_code") private String groupCode;
    @DbColumn("status") private ContentStatus status;
    @DbColumn("featured_media_id") private Media featuredMedia;
    @DbColumn("published_at") private Timestamp publishedAt;
    public ContentItem() { }
    public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public ContentType getType(){return type;} public void setType(ContentType v){type=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getGroupCode(){return groupCode;} public void setGroupCode(String v){groupCode=v;}
    public ContentStatus getStatus(){return status;} public void setStatus(ContentStatus v){status=v;}
    public Media getFeaturedMedia(){return featuredMedia;} public void setFeaturedMedia(Media v){featuredMedia=v;}
    public Timestamp getPublishedAt(){return publishedAt;} public void setPublishedAt(Timestamp v){publishedAt=v;}
}
