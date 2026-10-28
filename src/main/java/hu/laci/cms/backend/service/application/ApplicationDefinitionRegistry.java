package hu.laci.cms.backend.service.application;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
/**
 * Immutable whitelist of application types explicitly deployed by backend code.
 * Database configuration can enable a definition, but cannot define executable behavior.
 */
public class ApplicationDefinitionRegistry {
    private final Map<String, ApplicationDefinition> definitions;
    /**
     * Creates a registry from the explicitly configured definitions.
     *
     * @param definitions supported definitions
     * @throws IllegalArgumentException if a code is registered more than once
     */
    public ApplicationDefinitionRegistry(Collection<ApplicationDefinition> definitions) { Map<String,ApplicationDefinition> values=new LinkedHashMap<>(); for(ApplicationDefinition definition:definitions){ApplicationDefinition safe=Objects.requireNonNull(definition); if(values.put(safe.code(),safe)!=null) throw new IllegalArgumentException("Duplicate application definition: "+safe.code());} this.definitions=Map.copyOf(values); }
    /** @return an empty registry suitable for isolated tests and legacy service construction */
    public static ApplicationDefinitionRegistry empty() { return new ApplicationDefinitionRegistry(java.util.List.of()); }
    /**
     * Determines whether an application code is deployed.
     *
     * @param code application code from a trusted application row
     * @return {@code true} when the code is explicitly supported
     */
    public boolean isSupported(String code) { return code != null && definitions.containsKey(code); }
    /** @return trusted deployed metadata for the application code. */
    public Optional<ApplicationDefinition> find(String code) { return Optional.ofNullable(definitions.get(code)); }
}
