package hu.laci.cms.backend.dto.site;

/**
 * Blocking Site-owned resource counts used to explain whether a Site can be deleted.
 */
public record SiteUsageResponse(long pageCount, long menuCount, long mediaCount, long contentItemCount,
                                long contentCollectionCount, boolean deletable) {
    /** Creates a backward-compatible usage response without Content Foundation counts. */
    public SiteUsageResponse(long pageCount, long menuCount, long mediaCount, boolean deletable) {
        this(pageCount, menuCount, mediaCount, 0, 0, deletable);
    }
}
