package hu.laci.cms.backend.service.application;
import hu.laci.cms.backend.service.function.AccessPolicy;
/** Explicit deployed application type metadata; database rows enable it but never define executable code. */
public record ApplicationDefinition(String code, AccessPolicy accessPolicy) {
    public ApplicationDefinition(String code) { this(code, AccessPolicy.PUBLIC); }
}
