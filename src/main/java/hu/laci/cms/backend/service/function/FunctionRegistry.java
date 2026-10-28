package hu.laci.cms.backend.service.function;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
/** Explicit pair-keyed whitelist preventing functions from being called under another application. */
public class FunctionRegistry {
    private final Map<Key, FunctionDefinition> functions;
    /**
     * Registers handlers by their exact application/function pair.
     *
     * @param handlers explicitly deployed handlers
     * @throws IllegalArgumentException if a pair is registered more than once
     */
    public FunctionRegistry(Collection<? extends PublicFunction> handlers) { Map<Key,FunctionDefinition> values=new LinkedHashMap<>(); for(PublicFunction handler:handlers){PublicFunction safe=Objects.requireNonNull(handler);Key key=new Key(safe.getApplicationCode(),safe.getFunctionCode());if(values.put(key,new FunctionDefinition(safe.getApplicationCode(),safe.getFunctionCode(),safe.getAccessPolicy(),safe))!=null)throw new IllegalArgumentException("Duplicate public function registration.");}functions=Map.copyOf(values); }
    /** @return an empty registry with no callable functions */
    public static FunctionRegistry empty(){return new FunctionRegistry(java.util.List.of());}
    /**
     * Resolves only the handler registered for both supplied codes.
     *
     * @param applicationCode owning application code
     * @param functionCode function code inside that application namespace
     * @return matching handler when explicitly registered
     */
    public Optional<PublicFunction> find(String applicationCode,String functionCode){return findDefinition(applicationCode,functionCode).map(FunctionDefinition::handler);}
    /** @return trusted definition for the exact pair when explicitly registered */
    public Optional<FunctionDefinition> findDefinition(String applicationCode,String functionCode){return Optional.ofNullable(functions.get(new Key(applicationCode,functionCode)));}
    /** Policy-bearing handler registration from trusted source code. */
    public record FunctionDefinition(String applicationCode,String functionCode,AccessPolicy accessPolicy,PublicFunction handler) { }
    private record Key(String applicationCode,String functionCode) { }
}
