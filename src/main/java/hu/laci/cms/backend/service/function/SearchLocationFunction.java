package hu.laci.cms.backend.service.function;
import com.google.gson.*;
import hu.laci.cms.backend.dto.weather.*;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.weather.*;
import java.util.Objects;
/** Transport adapter for the public WEATHER_EXPLORER/SEARCH_LOCATION function. */
public final class SearchLocationFunction implements PublicFunction {
    public static final String APPLICATION_CODE = "WEATHER_EXPLORER"; public static final String FUNCTION_CODE = "SEARCH_LOCATION";
    private final Gson gson; private final WeatherService weatherService;
    public SearchLocationFunction() { this(new Gson(), WeatherServiceFactory.getInstance()); }
    public SearchLocationFunction(Gson gson, WeatherService weatherService) { this.gson = Objects.requireNonNull(gson); this.weatherService = Objects.requireNonNull(weatherService); }
    @Override public String getApplicationCode() { return APPLICATION_CODE; } @Override public String getFunctionCode() { return FUNCTION_CODE; } @Override public AccessPolicy getAccessPolicy() { return AccessPolicy.PUBLIC; }
    @Override public SearchLocationResponse execute(FunctionContext context, JsonObject payload) { try { SearchLocationRequest request = gson.fromJson(payload, SearchLocationRequest.class); return weatherService.searchLocations(request); } catch (PublicFunctionException exception) { throw exception; } catch (ExternalApiException exception) { throw new PublicFunctionException(exception.getCode(), exception.getMessage()); } catch (RuntimeException exception) { throw new PublicFunctionException("INVALID_FUNCTION_INPUT", "Invalid location search request."); } }
}
