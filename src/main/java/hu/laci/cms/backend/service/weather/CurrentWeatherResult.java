package hu.laci.cms.backend.service.weather;
/** Provider-neutral current conditions. */
public record CurrentWeatherResult(double temperature, double apparentTemperature, int relativeHumidity, double windSpeed, int weatherCode) { }
