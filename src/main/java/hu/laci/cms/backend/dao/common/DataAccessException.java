package hu.laci.cms.backend.dao.common;

/**
 * Runtime exception used by DAO code to wrap low-level SQL failures.
 * <p>
 * DAO callers should catch this only when they can add useful business-level
 * handling; otherwise servlet-level exception handling turns it into a common
 * API error response.
 */
public class DataAccessException extends RuntimeException {

    /**
     * Creates a data access exception with message only.
     *
     * @param message error message safe for upper layers
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Creates a data access exception wrapping the original failure.
     *
     * @param message error message safe for upper layers
     * @param cause original SQL or mapping failure
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
