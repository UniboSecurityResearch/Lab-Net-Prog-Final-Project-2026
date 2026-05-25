package p4_aas.Submodels.NetworkInfrastructure;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import p4_aas.Submodels.SwitchRuntime.SwitchCliClient;

public class NetworkInfrastructureLambda {

    private final SwitchCliClient switchCliClient;

    public NetworkInfrastructureLambda() {
        this.switchCliClient = new SwitchCliClient();
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> configureFc1RateLimit() {
        return (args) -> {
            int sw = getInt(args, "Switch");
            int threshold = getInt(args, "Threshold");
            int enabled = getInt(args, "Enabled");

            switchCliClient.runCliCommand(sw, "register_write fc1_threshold 0 " + threshold);
            switchCliClient.runCliCommand(sw, "register_write fc1_blocking_enabled 0 " + enabled);
            switchCliClient.runCliCommand(sw, "register_write fc1_packet_count 0 0");

            return output("OK: threshold=" + threshold + " enabled=" + enabled);
        };
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> getFc1PacketCount() {
        return (args) -> {
            int sw = getInt(args, "Switch");
            String result = switchCliClient.runCliCommand(sw, "register_read fc1_packet_count 0");
            return output(result);
        };
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> resetFc1Counter() {
        return (args) -> {
            int sw = getInt(args, "Switch");
            switchCliClient.runCliCommand(sw, "register_write fc1_packet_count 0 0");
            return output("Counter reset to 0");
        };
    }

    private SubmodelElement[] output(String value) {
        return new SubmodelElement[] { new Property("Output", value) };
    }

    private int getInt(Map<String, SubmodelElement> args, String name) {
        Object value = args.get(name).getValue();
        if (value instanceof BigInteger) return ((BigInteger) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
