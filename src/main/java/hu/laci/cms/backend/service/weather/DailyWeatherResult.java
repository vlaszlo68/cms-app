package hu.laci.cms.backend.service.weather;

/** Provider-neutral detailed forecast for one selected-location local calendar day. */
public record DailyWeatherResult(String date, Integer weatherCode, Double temperatureMin, Double temperatureMax,
                                 Double apparentTemperatureMin, Double apparentTemperatureMax,
                                 Integer precipitationProbability, Double precipitationSum, Double rainSum,
                                 Double snowfallSum, Double windSpeedMax, Double windGustsMax,
                                 Integer windDirectionDominant, String sunrise, String sunset,
                                 Double daylightDurationSeconds, Double sunshineDurationSeconds) { }
