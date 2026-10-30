package hu.laci.cms.backend.dto.weather;

import java.util.List;
/** Public location-search response. */
public record SearchLocationResponse(List<LocationDto> locations) { public SearchLocationResponse { locations = List.copyOf(locations); } }
