package hu.laci.cms.backend.dto.weather;

import java.util.List;

/** Public response containing current, hourly, and daily weather forecasts. */
public record GetWeatherResponse(CurrentWeatherDto current, List<HourlyWeatherDto> hourly, List<DailyWeatherDto> daily) {
    public GetWeatherResponse {
        hourly = List.copyOf(hourly);
        daily = List.copyOf(daily);
    }
}
