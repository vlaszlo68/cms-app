package hu.laci.cms.backend.dao.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an entity field to a database column.
 * <p>
 * The annotation is read by {@code BaseDao} when building SQL and mapping
 * {@code ResultSet} rows back to entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbColumn {

    /**
     * Database column name.
     *
     * @return column name, for example {@code login_name}
     */
    String value();

    /**
     * Whether the column is included in generated INSERT statements.
     *
     * @return true when insertable
     */
    boolean insertable() default true;

    /**
     * Whether the column is included in generated UPDATE statements.
     *
     * @return true when updatable
     */
    boolean updatable() default true;
}
