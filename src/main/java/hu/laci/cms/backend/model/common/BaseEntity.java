package hu.laci.cms.backend.model.common;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;

/**
 * Base class for all database-backed entities.
 * <p>
 * The DAO infrastructure assumes that every entity has a {@link Long} primary
 * key named {@code id}. Subclasses add their own {@link DbColumn}-annotated
 * fields.
 */
public abstract class BaseEntity {

    @DbColumn("id")
    private Long id;

    /**
     * Creates a base entity.
     */
    protected BaseEntity() {
    }

    /**
     * Returns the database primary key.
     *
     * @return the entity id, or {@code null} for a not-yet-persisted entity
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database primary key.
     * <p>
     * DAO {@code create} methods set this after insert. Application code should
     * normally leave it {@code null} for new entities.
     *
     * @param id the database id
     */
    public void setId(Long id) {
        this.id = id;
    }
}
