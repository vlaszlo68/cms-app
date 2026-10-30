package hu.laci.cms.backend.dto.weather;

/** Provider-independent public location result. */
public record LocationDto(Long id, String name, String country, String countryCode, String admin1, double latitude, double longitude, String timezone) { }
