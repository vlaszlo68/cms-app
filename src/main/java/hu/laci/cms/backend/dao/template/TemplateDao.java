package hu.laci.cms.backend.dao.template;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.template.TemplateProperty;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for template configuration records.
 */
public interface TemplateDao extends CrudDao<Template, TemplateProperty> {

    Optional<Template> findByCode(String code);

    List<Template> findActive();
}
