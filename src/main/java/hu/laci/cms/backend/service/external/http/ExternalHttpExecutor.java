package hu.laci.cms.backend.service.external.http;

/** Reusable HTTP-only boundary for trusted third-party REST clients. */
public interface ExternalHttpExecutor {
    /** Executes a preconstructed trusted request. @param request transport request @return successful response @throws ExternalApiException controlled outbound failure */
    ExternalHttpResponse execute(ExternalHttpRequest request);
}
