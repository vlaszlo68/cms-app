package hu.laci.cms.backend.service.weather;
/** Provider-neutral location returned by a weather provider. */
public record LocationResult(Long id, String name, String country, String countryCode, String admin1, double latitude, double longitude, String timezone) { }
