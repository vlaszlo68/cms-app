package hu.laci.cms.backend.service.site;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable whitelist of logical Site themes. It deliberately contains no stylesheet implementation.
 */
public final class ThemeDefinitionRegistry {

    public static final String STANDARD = "STANDARD";
    private static final List<ThemeDefinition> DEFINITIONS = List.of(
            new ThemeDefinition(STANDARD, "Standard"),
            new ThemeDefinition("MODERN", "Modern"),
            new ThemeDefinition("DARK", "Dark"),
            new ThemeDefinition("CORPORATE", "Corporate"),
            new ThemeDefinition("MINIMAL", "Minimal"));
    private static final Map<String, ThemeDefinition> BY_CODE = DEFINITIONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(ThemeDefinition::code, definition -> definition));

    private ThemeDefinitionRegistry() {
    }

    /** @return all supported logical theme definitions in display order */
    public static List<ThemeDefinition> definitions() {
        return DEFINITIONS;
    }

    /** @param code logical theme code @return definition when supported */
    public static Optional<ThemeDefinition> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
