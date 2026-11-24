package rs.sep.psp.plugin;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Svi plugin-ovi koji su trenutno ukljuceni u PSP, po kodu nacina placanja. */
@Component
public class PluginRegistry {

    private final Map<String, PaymentPlugin> plugins;

    public PluginRegistry(List<PaymentPlugin> plugins) {
        this.plugins = plugins.stream().collect(Collectors.toMap(PaymentPlugin::code, Function.identity()));
    }

    public Optional<PaymentPlugin> find(String code) {
        return Optional.ofNullable(plugins.get(code));
    }

    public boolean supports(String code) {
        return plugins.containsKey(code);
    }
}
