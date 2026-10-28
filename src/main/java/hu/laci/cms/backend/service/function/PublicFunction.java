package hu.laci.cms.backend.service.function;
import com.google.gson.JsonObject;
/**
 * Explicitly registered public application function.
 * Implementations should deserialize the transport payload into a typed request DTO before calling a service.
 */
public interface PublicFunction {
    /** @return application code that owns this function */
    String getApplicationCode();
    /** @return function code unique within the owning application */
    String getFunctionCode();
    /** @return trusted minimum policy required to invoke this handler */
    default AccessPolicy getAccessPolicy() { return AccessPolicy.PUBLIC; }
    /**
     * Executes this function with validated Site/Application context.
     *
     * @param context resolved domain context
     * @param payload JSON transport object to map into a typed request DTO
     * @return a typed response DTO or other Gson-serializable result
     * @throws PublicFunctionException for controlled function validation or business errors
     */
    Object execute(FunctionContext context, JsonObject payload);
}
