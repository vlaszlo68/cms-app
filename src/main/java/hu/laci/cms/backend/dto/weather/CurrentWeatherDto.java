package hu.laci.cms.backend.dto.weather;

/** Provider-independent current weather values; unavailable measurements remain {@code null}. */
public record CurrentWeatherDto(Double temperature, Double apparentTemperature, Integer relativeHumidity,
                                Integer weatherCode, Double precipitation, Double rain, Double snowfall,
                                Integer cloudCover, Double pressureMsl, Double windSpeed,
                                Integer windDirection, Double windGusts, Boolean isDay) { }
