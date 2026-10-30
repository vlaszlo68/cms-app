package hu.laci.cms.backend.service.function;
import com.google.gson.*;
import hu.laci.cms.backend.dto.weather.*;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.weather.*;
import java.util.Objects;
/** Transport adapter for the public WEATHER_EXPLORER/GET_WEATHER function. */
public final class GetWeatherFunction implements PublicFunction {
    public static final String APPLICATION_CODE = SearchLocationFunction.APPLICATION_CODE; public static final String FUNCTION_CODE = "GET_WEATHER";
    private final Gson gson; private final WeatherService weatherService;
    public GetWeatherFunction() { this(new Gson(), WeatherServiceFactory.getInstance()); }
    public GetWeatherFunction(Gson gson, WeatherService weatherService) { this.gson = Objects.requireNonNull(gson); this.weatherService = Objects.requireNonNull(weatherService); }
    @Override public String getApplicationCode() { return APPLICATION_CODE; } @Override public String getFunctionCode() { return FUNCTION_CODE; } @Override public AccessPolicy getAccessPolicy() { return AccessPolicy.PUBLIC; }
    @Override public GetWeatherResponse execute(FunctionContext context, JsonObject payload) { try { GetWeatherRequest request = gson.fromJson(payload, GetWeatherRequest.class); return weatherService.getWeather(request); } catch (PublicFunctionException exception) { throw exception; } catch (ExternalApiException exception) { throw new PublicFunctionException(exception.getCode(), exception.getMessage()); } catch (RuntimeException exception) { throw new PublicFunctionException("INVALID_FUNCTION_INPUT", "Invalid weather request."); } }
}
