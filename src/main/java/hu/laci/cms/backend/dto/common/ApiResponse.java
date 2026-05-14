package hu.laci.cms.backend.dto.common;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiErrorResponse error;

    private ApiResponse(boolean success, T data, ApiErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(ApiErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ApiErrorResponse getError() {
        return error;
    }
}
