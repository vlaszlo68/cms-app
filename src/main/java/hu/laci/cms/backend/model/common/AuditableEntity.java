package hu.laci.cms.backend.model.common;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;

import java.sql.Timestamp;

public abstract class AuditableEntity extends BaseEntity {

    @DbColumn(value = "created_at", updatable = false)
    private Timestamp createdAt;

    @DbColumn("updated_at")
    private Timestamp updatedAt;

    @DbColumn(value = "created_by", updatable = false)
    private Long createdBy;

    @DbColumn("updated_by")
    private Long updatedBy;

    protected AuditableEntity() {
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
