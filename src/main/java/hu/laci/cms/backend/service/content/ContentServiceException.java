package hu.laci.cms.backend.service.content;
/** Typed business failure returned by Content Foundation services. */ public class ContentServiceException extends RuntimeException { private final String code; public ContentServiceException(String code,String message){super(message);this.code=code;} public String getCode(){return code;} }
