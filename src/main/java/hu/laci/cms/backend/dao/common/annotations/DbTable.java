package hu.laci.cms.backend.dao.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an entity class to a database table.
 * <p>
 * The annotation is required by {@code BaseDao} for generated CRUD and
 * {@link hu.laci.cms.backend.model.common.QuerySpec}-based SQL.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DbTable {

    /**
     * Database table name.
     *
     * @return table name, for example {@code users}
     */
    String value();
}
