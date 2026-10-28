package hu.laci.cms.backend.service.function;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
/** Explicit pair-keyed whitelist preventing functions from being called under another application. */
public class FunctionRegistry {
    private final Map<Key, PublicFunction> functions;
    /**
     * Registers handlers by their exact application/function pair.
     *
     * @param handlers explicitly deployed handlers
     * @throws IllegalArgumentException if a pair is registered more than once
     */
    public FunctionRegistry(Collection<? extends PublicFunction> handlers) { Map<Key,PublicFunction> values=new LinkedHashMap<>(); for(PublicFunction handler:handlers){PublicFunction safe=Objects.requireNonNull(handler);Key key=new Key(safe.getApplicationCode(),safe.getFunctionCode());if(values.put(key,safe)!=null)throw new IllegalArgumentException("Duplicate public function registration.");}functions=Map.copyOf(values); }
    /** @return an empty registry with no callable functions */
    public static FunctionRegistry empty(){return new FunctionRegistry(java.util.List.of());}
    /**
     * Resolves only the handler registered for both supplied codes.
     *
     * @param applicationCode owning application code
     * @param functionCode function code inside that application namespace
     * @return matching handler when explicitly registered
     */
    public Optional<PublicFunction> find(String applicationCode,String functionCode){return Optional.ofNullable(functions.get(new Key(applicationCode,functionCode)));}
    private record Key(String applicationCode,String functionCode) { }
}
