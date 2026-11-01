package hu.laci.cms.backend.dto.content;

import hu.laci.cms.backend.model.content.ContentType;
import java.util.List;

/** Safe public summary or article representation of published editorial content. */
public record PublicContentItemResponse(ContentType type, String slug, String title, String summary,
                                        String content, String groupCode, String publishedAt,
                                        Long featuredMediaId, List<String> tags) {
}
