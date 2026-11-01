package hu.laci.cms.backend.model.page;

/** Validated persisted configuration for a {@code CONTENT_COLLECTION} PageBlock. */
public class ContentCollectionBlockConfig {
    private String collectionCode;
    private ContentCollectionDisplayMode displayMode = ContentCollectionDisplayMode.CARDS;
    private Boolean paginationEnabled = Boolean.TRUE;

    public String getCollectionCode() { return collectionCode; }
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
    public ContentCollectionDisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(ContentCollectionDisplayMode displayMode) { this.displayMode = displayMode; }
    public Boolean getPaginationEnabled() { return paginationEnabled; }
    public void setPaginationEnabled(Boolean paginationEnabled) { this.paginationEnabled = paginationEnabled; }
}
