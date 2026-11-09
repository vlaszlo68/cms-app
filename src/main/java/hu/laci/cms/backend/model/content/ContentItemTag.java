package hu.laci.cms.backend.model.content;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.BaseEntity;

/** Typed join entity for a content item's assigned Site-owned tag. */
@DbTable("content_item_tags")
public class ContentItemTag extends BaseEntity {
    @DbColumn("content_item_id")
    private ContentItem contentItem;
    @DbColumn("tag_id")
    private Tag tag;

    /** Returns the item receiving the tag assignment. */
    public ContentItem getContentItem() { return contentItem; }
    /** Sets the item receiving the tag assignment. */
    public void setContentItem(ContentItem contentItem) { this.contentItem = contentItem; }
    /** Returns the assigned tag. */
    public Tag getTag() { return tag; }
    /** Sets the assigned tag. */
    public void setTag(Tag tag) { this.tag = tag; }
}
