package hu.laci.cms.backend.dto.content;

import java.util.List;

/** One zero-based server-side page of publicly visible collection content. */
public record PublicContentCollectionResponse(List<PublicContentItemResponse> items, int page,
                                              int pageSize, long totalElements, long totalPages) {
}
