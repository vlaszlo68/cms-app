package hu.laci.cms.backend.service.weather;

import java.util.List;

/** Provider-neutral complete weather result cached as one atomic forecast snapshot. */
public record WeatherResult(CurrentWeatherResult current, List<HourlyWeatherResult> hourly, List<DailyWeatherResult> daily) {
    public WeatherResult {
        hourly = List.copyOf(hourly);
        daily = List.copyOf(daily);
    }
}
