package hu.laci.cms.backend.model.content;
import hu.laci.cms.backend.model.common.AuditableProperty; import hu.laci.cms.backend.model.common.BaseProperty;
/** Query properties for tags. */ public final class TagProperty extends AuditableProperty { public static final TagProperty ID=new TagProperty(BaseProperty.ID.getPropertyName()); public static final TagProperty SITE=new TagProperty("site"); public static final TagProperty CODE=new TagProperty("code"); private TagProperty(String n){super(Tag.class,n);} }
