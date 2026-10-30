package hu.laci.cms.backend.dto.weather;
import java.util.List;
/** Public response containing present conditions and daily forecasts. */
public record GetWeatherResponse(CurrentWeatherDto current, List<DailyWeatherDto> daily) { public GetWeatherResponse { daily = List.copyOf(daily); } }
