package hu.laci.cms.backend.dto.site;

/**
 * Blocking Site-owned resource counts used to explain whether a Site can be deleted.
 */
public record SiteUsageResponse(long pageCount, long menuCount, long mediaCount,
                                long applicationCount, boolean deletable) {
}
