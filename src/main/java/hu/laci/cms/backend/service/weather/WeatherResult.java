package hu.laci.cms.backend.service.weather;
import java.util.List;
/** Provider-neutral complete weather result. */
public record WeatherResult(CurrentWeatherResult current, List<DailyWeatherResult> daily) { public WeatherResult { daily = List.copyOf(daily); } }
