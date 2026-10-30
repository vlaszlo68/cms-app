package hu.laci.cms.backend.service.weather;
/** Provider-neutral daily weather result. */
public record DailyWeatherResult(String date, int weatherCode, double temperatureMin, double temperatureMax, int precipitationProbability) { }
