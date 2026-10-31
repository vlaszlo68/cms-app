package hu.laci.cms.backend.service.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hu.laci.cms.backend.service.external.cache.InMemoryTtlCache;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.weather.WeatherProviderClient;
import hu.laci.cms.backend.service.weather.WeatherService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetWeatherFunctionTest {
    @Test
    void mapsProviderFailureToStablePublicFunctionErrorAndRejectsInvalidCoordinates() {
        WeatherProviderClient provider = new WeatherProviderClient() {
            @Override public List<hu.laci.cms.backend.service.weather.LocationResult> searchLocations(String query, String language) { return List.of(); }
            @Override public hu.laci.cms.backend.service.weather.WeatherResult getWeather(double latitude, double longitude, String timezone) { throw new ExternalApiException(ExternalApiException.UNAVAILABLE, "External service is unavailable."); }
        };
        GetWeatherFunction function = new GetWeatherFunction(new Gson(), new WeatherService(provider, new InMemoryTtlCache<>(), new InMemoryTtlCache<>()));
        JsonObject valid = new JsonObject(); valid.addProperty("latitude", 47.5); valid.addProperty("longitude", 19.0);
        assertEquals(ExternalApiException.UNAVAILABLE, assertThrows(PublicFunctionException.class, () -> function.execute(null, valid)).getCode());
        JsonObject invalid = new JsonObject(); invalid.addProperty("latitude", 91); invalid.addProperty("longitude", 19);
        assertEquals("INVALID_FUNCTION_INPUT", assertThrows(PublicFunctionException.class, () -> function.execute(null, invalid)).getCode());
    }
}
