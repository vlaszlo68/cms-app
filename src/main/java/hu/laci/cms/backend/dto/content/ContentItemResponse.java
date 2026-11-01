package hu.laci.cms.backend.dto.content;
import hu.laci.cms.backend.model.content.*; import java.util.*;
/** Read representation of one content item including its assigned tags. */ public record ContentItemResponse(Long id,ContentType type,String title,String slug,String summary,String content,String groupCode,ContentStatus status,Long featuredMediaId,String publishedAt,List<TagResponse> tags,String createdAt,String updatedAt){}
