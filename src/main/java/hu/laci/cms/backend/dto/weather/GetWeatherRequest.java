package hu.laci.cms.backend.dto.weather;

/** Public request for current and seven-day weather at a selected coordinate. */
public final class GetWeatherRequest { private Double latitude; private Double longitude; private String timezone; public Double getLatitude() { return latitude; } public void setLatitude(Double latitude) { this.latitude = latitude; } public Double getLongitude() { return longitude; } public void setLongitude(Double longitude) { this.longitude = longitude; } public String getTimezone() { return timezone; } public void setTimezone(String timezone) { this.timezone = timezone; } }
