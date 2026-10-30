package hu.laci.cms.backend.service.weather.openmeteo;

import java.util.List;

/** Vendor-only Open-Meteo geocoding response shape, kept inside the provider adapter package. */
final class OpenMeteoGeocodingResponse {
    List<OpenMeteoLocation> results;

    /** Vendor-only Open-Meteo geocoding result shape. */
    static final class OpenMeteoLocation {
        Long id;
        String name;
        String country;
        String country_code;
        String admin1;
        Double latitude;
        Double longitude;
        String timezone;
    }
}
