package hu.laci.cms.backend.dto.content;

import hu.laci.cms.backend.model.content.ContentType;
import java.util.List;

/** Safe anonymous listing representation of published editorial content. */
public record PublicContentPreviewResponse(ContentType type, String slug, String title, String summary,
                                           String publishedAt, Object featuredMedia, List<String> tags) { }
