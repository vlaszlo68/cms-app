package hu.laci.cms.backend.dto.application;
/** Minimal anonymous descriptor; it intentionally omits audit and configuration fields. */
public class PublicApplicationResponse { private final String code; private final String name; public PublicApplicationResponse(String code,String name){this.code=code;this.name=name;} public String getCode(){return code;} public String getName(){return name;} }
