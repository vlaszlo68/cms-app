package hu.laci.cms.backend.dao.template;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.template.TemplateProperty;

import java.util.List;
import java.util.Optional;

/**
 * JDBC template DAO backed by the annotation-driven {@link BaseDao}.
 */
public class TemplateDaoImpl extends BaseDao<Template, TemplateProperty> implements TemplateDao {

    public TemplateDaoImpl() {
        super(Template.class);
    }

    @Override
    public Optional<Template> findByCode(String code) {
        return findOneByProperty("code", code, "Failed to find template by code: " + code);
    }

    @Override
    public List<Template> findActive() {
        return findAll(QuerySpec.<TemplateProperty>create()
                .where(TemplateProperty.ACTIVE).equalsTo(Boolean.TRUE)
                .orderBy(TemplateProperty.NAME.asc()));
    }
}
