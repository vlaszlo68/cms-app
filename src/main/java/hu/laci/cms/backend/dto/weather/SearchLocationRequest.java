package hu.laci.cms.backend.dto.weather;

/** Public request for a location search. */
public final class SearchLocationRequest { private String query; private String language; public String getQuery() { return query; } public void setQuery(String query) { this.query = query; } public String getLanguage() { return language; } public void setLanguage(String language) { this.language = language; } }
