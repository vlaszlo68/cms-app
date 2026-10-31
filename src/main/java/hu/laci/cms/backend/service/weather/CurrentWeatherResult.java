package hu.laci.cms.backend.service.weather;

/** Provider-neutral current conditions; unavailable provider measurements remain {@code null}. */
public record CurrentWeatherResult(Double temperature, Double apparentTemperature, Integer relativeHumidity,
                                   Integer weatherCode, Double precipitation, Double rain, Double snowfall,
                                   Integer cloudCover, Double pressureMsl, Double windSpeed,
                                   Integer windDirection, Double windGusts, Boolean isDay) { }
