package hu.laci.cms.backend.service.weather.openmeteo;

import java.util.List;

/** Vendor-only Open-Meteo forecast response shape, kept inside the provider adapter package. */
final class OpenMeteoForecastResponse {
    OpenMeteoCurrent current;
    OpenMeteoUnits current_units;
    OpenMeteoHourly hourly;
    OpenMeteoUnits hourly_units;
    OpenMeteoDaily daily;
    OpenMeteoUnits daily_units;

    /** Vendor-only current conditions shape. */
    static final class OpenMeteoCurrent {
        Double temperature_2m;
        Double apparent_temperature;
        Integer relative_humidity_2m;
        Integer weather_code;
        Double precipitation;
        Double rain;
        Double snowfall;
        Integer cloud_cover;
        Double pressure_msl;
        Double wind_speed_10m;
        Integer wind_direction_10m;
        Double wind_gusts_10m;
        Integer is_day;
    }

    /** Vendor-only hourly parallel arrays. */
    static final class OpenMeteoHourly {
        List<String> time;
        List<Double> temperature_2m;
        List<Double> apparent_temperature;
        List<Integer> relative_humidity_2m;
        List<Integer> precipitation_probability;
        List<Double> precipitation;
        List<Integer> weather_code;
        List<Integer> cloud_cover;
        List<Double> wind_speed_10m;
        List<Integer> wind_direction_10m;
        List<Double> wind_gusts_10m;
    }

    /** Vendor-only parallel daily arrays. */
    static final class OpenMeteoDaily {
        List<String> time;
        List<Integer> weather_code;
        List<Double> temperature_2m_min;
        List<Double> temperature_2m_max;
        List<Double> apparent_temperature_min;
        List<Double> apparent_temperature_max;
        List<Integer> precipitation_probability_max;
        List<Double> precipitation_sum;
        List<Double> rain_sum;
        List<Double> snowfall_sum;
        List<Double> wind_speed_10m_max;
        List<Double> wind_gusts_10m_max;
        List<Integer> wind_direction_10m_dominant;
        List<String> sunrise;
        List<String> sunset;
        List<Double> daylight_duration;
        List<Double> sunshine_duration;
    }

    /** Vendor-only unit metadata shape, retained for Gson compatibility without public exposure. */
    static final class OpenMeteoUnits { }
}
