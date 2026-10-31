package hu.laci.cms.backend.dto.weather;

/** Provider-independent forecast for one location-local hour. */
public record HourlyWeatherDto(String time, Double temperature, Double apparentTemperature,
                               Integer relativeHumidity, Integer precipitationProbability,
                               Double precipitation, Integer weatherCode, Integer cloudCover,
                               Double windSpeed, Integer windDirection, Double windGusts) { }
