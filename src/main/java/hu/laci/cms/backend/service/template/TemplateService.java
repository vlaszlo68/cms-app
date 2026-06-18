package hu.laci.cms.backend.service.template;

import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.template.CreateTemplateRequest;
import hu.laci.cms.backend.dto.template.TemplateRequestBase;
import hu.laci.cms.backend.dto.template.TemplateResponse;
import hu.laci.cms.backend.dto.template.UpdateTemplateRequest;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.template.TemplateProperty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business service for frontend template configuration management.
 */
public class TemplateService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String DUPLICATE_CODE = "DUPLICATE_CODE";

    private final TemplateDao templateDao;

    public TemplateService(TemplateDao templateDao) {
        this.templateDao = Objects.requireNonNull(templateDao, "templateDao must not be null");
    }

    public List<TemplateResponse> listTemplates() {
        return templateDao.findAll(QuerySpec.<TemplateProperty>create().orderBy(TemplateProperty.NAME.asc()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TemplateResponse getTemplate(Long id) {
        return toResponse(loadTemplate(id));
    }

    public TemplateResponse findByCode(String code) {
        if (isBlank(code)) {
            throw new TemplateServiceException(VALIDATION_ERROR, "code is required.");
        }
        return templateDao.findByCode(code.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new TemplateServiceException(TEMPLATE_NOT_FOUND, "Template not found."));
    }

    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        validate(request);
        String code = request.getCode().trim();
        ensureCodeAvailable(code, null);
        Template template = new Template(null, code, request.getName().trim(),
                trimToNull(request.getDescription()), request.getPreviewImageMediaId(),
                request.getActive() == null || request.getActive());
        return toResponse(templateDao.create(template));
    }

    public TemplateResponse updateTemplate(Long id, UpdateTemplateRequest request) {
        validate(request);
        Template template = loadTemplate(id);
        String code = request.getCode().trim();
        ensureCodeAvailable(code, id);
        template.setCode(code);
        template.setName(request.getName().trim());
        template.setDescription(trimToNull(request.getDescription()));
        template.setPreviewImageMediaId(request.getPreviewImageMediaId());
        template.setActive(request.getActive() == null || request.getActive());
        return toResponse(templateDao.update(template));
    }

    public TemplateResponse deactivateTemplate(Long id) {
        Template template = loadTemplate(id);
        template.setActive(false);
        return toResponse(templateDao.update(template));
    }

    private Template loadTemplate(Long id) {
        if (id == null) {
            throw new TemplateServiceException(VALIDATION_ERROR, "Template id is required.");
        }
        return templateDao.findById(id)
                .orElseThrow(() -> new TemplateServiceException(TEMPLATE_NOT_FOUND, "Template not found."));
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        Optional<Template> existing = templateDao.findByCode(code);
        if (existing.isPresent() && !existing.get().getId().equals(currentId)) {
            throw new TemplateServiceException(DUPLICATE_CODE, "code is already used.");
        }
    }

    private static void validate(TemplateRequestBase request) {
        if (request == null) {
            throw new TemplateServiceException(VALIDATION_ERROR, "Request body is required.");
        }
        if (isBlank(request.getCode())) {
            throw new TemplateServiceException(VALIDATION_ERROR, "code is required.");
        }
        if (isBlank(request.getName())) {
            throw new TemplateServiceException(VALIDATION_ERROR, "name is required.");
        }
    }

    private TemplateResponse toResponse(Template template) {
        return new TemplateResponse(template.getId(), template.getCode(), template.getName(),
                template.getDescription(), template.getPreviewImageMediaId(), template.isActive());
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
