package hu.laci.cms.backend.service.function;
/** Controlled dispatch failure that is safe to expose via the API envelope. */
public class PublicFunctionException extends RuntimeException { private final String code; public PublicFunctionException(String code,String message){super(message);this.code=code;} public String getCode(){return code;} }
