package hu.laci.cms.backend.service.weather;

/** Provider-neutral forecast for one selected-location local hour. */
public record HourlyWeatherResult(String time, Double temperature, Double apparentTemperature,
                                  Integer relativeHumidity, Integer precipitationProbability,
                                  Double precipitation, Integer weatherCode, Integer cloudCover,
                                  Double windSpeed, Integer windDirection, Double windGusts) { }
