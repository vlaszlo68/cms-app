package hu.laci.cms.backend.dto.common;

/**
 * Common API response envelope used by JSON endpoints.
 *
 * @param <T> success payload type
 */
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiErrorResponse error;

    private ApiResponse(boolean success, T data, ApiErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * Creates a successful API response.
     *
     * @param data response payload
     * @param <T> payload type
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Creates an error API response.
     *
     * @param error error payload
     * @return error response
     */
    public static ApiResponse<Void> error(ApiErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    /**
     * Returns whether the response represents success.
     *
     * @return success flag
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the success payload.
     *
     * @return payload, or {@code null} for error responses
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the error payload.
     *
     * @return error payload, or {@code null} for success responses
     */
    public ApiErrorResponse getError() {
        return error;
    }
}
