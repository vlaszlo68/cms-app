package hu.laci.cms.backend.service.application;
/** Explicit deployed application type metadata; database rows enable it but never define executable code. */
public record ApplicationDefinition(String code) { }
