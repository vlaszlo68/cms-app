package hu.laci.cms.backend.service.weather.openmeteo;

import java.util.List;

/** Vendor-only Open-Meteo forecast response shape, kept inside the provider adapter package. */
final class OpenMeteoForecastResponse {
    OpenMeteoCurrent current;
    OpenMeteoDaily daily;

    /** Vendor-only current conditions shape. */
    static final class OpenMeteoCurrent {
        Double temperature_2m;
        Double apparent_temperature;
        Integer relative_humidity_2m;
        Double wind_speed_10m;
        Integer weather_code;
    }

    /** Vendor-only parallel daily arrays. */
    static final class OpenMeteoDaily {
        List<String> time;
        List<Integer> weather_code;
        List<Double> temperature_2m_min;
        List<Double> temperature_2m_max;
        List<Integer> precipitation_probability_max;
    }
}
