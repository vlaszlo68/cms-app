package hu.laci.cms.backend.service.weather;
import java.util.List;
/** Provider port isolating WeatherService and public contracts from vendor transport and JSON. */
public interface WeatherProviderClient {
    List<LocationResult> searchLocations(String query, String language);
    WeatherResult getWeather(double latitude, double longitude, String timezone);
}
