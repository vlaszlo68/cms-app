package hu.laci.cms.backend.dto.weather;

/** Provider-independent detailed forecast for one location-local calendar day. */
public record DailyWeatherDto(String date, Integer weatherCode, Double temperatureMin, Double temperatureMax,
                              Double apparentTemperatureMin, Double apparentTemperatureMax,
                              Integer precipitationProbability, Double precipitationSum, Double rainSum,
                              Double snowfallSum, Double windSpeedMax, Double windGustsMax,
                              Integer windDirectionDominant, String sunrise, String sunset,
                              Double daylightDurationSeconds, Double sunshineDurationSeconds) { }
