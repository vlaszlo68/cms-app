package hu.laci.cms.backend.dto.content;
import hu.laci.cms.backend.model.content.*;
/** Read representation of a saved content collection definition. */ public record ContentCollectionResponse(Long id,String code,String name,ContentType contentType,String groupCode,ContentSort sort,Integer pageSize,Boolean active,String createdAt,String updatedAt){}
