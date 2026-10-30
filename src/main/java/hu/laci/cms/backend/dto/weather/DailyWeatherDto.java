package hu.laci.cms.backend.dto.weather;
/** Provider-independent weather summary for one local calendar day. */
public record DailyWeatherDto(String date, int weatherCode, double temperatureMin, double temperatureMax, int precipitationProbability) { }
