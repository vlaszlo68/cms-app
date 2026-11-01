package hu.laci.cms.backend.dto.content;

import hu.laci.cms.backend.model.content.ContentType;
import java.util.List;

/** Safe anonymous detail representation of one published NEWS or BLOG article. */
public record PublicArticleResponse(ContentType type, String slug, String title, String summary, String content,
                                    String publishedAt, Object featuredMedia, List<String> tags, String groupCode) { }
