package hu.laci.cms.backend.dto.application;
import hu.laci.cms.backend.service.function.AccessPolicy;
/** Minimal anonymous descriptor; it intentionally omits audit and configuration fields. */
public class PublicApplicationResponse { private final String code; private final String name; private final AccessPolicy accessPolicy; public PublicApplicationResponse(String code,String name){this(code,name,AccessPolicy.PUBLIC);} public PublicApplicationResponse(String code,String name,AccessPolicy accessPolicy){this.code=code;this.name=name;this.accessPolicy=accessPolicy;} public String getCode(){return code;} public String getName(){return name;} public AccessPolicy getAccessPolicy(){return accessPolicy;} }
