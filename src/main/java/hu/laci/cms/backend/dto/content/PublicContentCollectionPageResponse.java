package hu.laci.cms.backend.dto.content;

import java.util.List;

/** One zero-based server page from an active public content collection. */
public record PublicContentCollectionPageResponse(List<PublicContentPreviewResponse> items, int page,
                                                  int pageSize, long totalElements, int totalPages) { }
