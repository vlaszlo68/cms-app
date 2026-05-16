package hu.laci.cms.backend.model.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FilterProperty {

    String entityProperty();

    FilterOperation operation() default FilterOperation.EQUALS;

    LikeFilterPosition likePosition() default LikeFilterPosition.STARTS_WITH;
}
