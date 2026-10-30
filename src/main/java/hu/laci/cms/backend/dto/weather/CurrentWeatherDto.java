package hu.laci.cms.backend.dto.weather;
/** Provider-independent current weather values. */
public record CurrentWeatherDto(double temperature, double apparentTemperature, int relativeHumidity, double windSpeed, int weatherCode) { }
