package hu.laci.cms.backend.service.application;
/** Controlled application-platform business exception. */
public class ApplicationServiceException extends RuntimeException { private final String code; public ApplicationServiceException(String code,String message){super(message);this.code=code;} public String getCode(){return code;} }
