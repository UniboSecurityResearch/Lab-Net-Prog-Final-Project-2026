package p4_aas.Submodels.NetworkInfrastructure;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

import p4_aas.StaticProperties;
import p4_aas.Submodels.SwitchRuntime.SwitchCliClient;

public class TrafficMirrorLambda {
    private static final Pattern MIRRORED_COUNT_PATTERN =
        Pattern.compile("mirrored_packet_count\\[0\\]\\s*=\\s*(\\d+)");

    private final SwitchCliClient switchCliClient;

    public TrafficMirrorLambda() {
        this.switchCliClient = new SwitchCliClient();
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> readMirroredPacketCount() {
        return (args) -> {
            String output = switchCliClient.runCliCommand(StaticProperties.OBSERVER_SWITCH, "register_read mirrored_packet_count 0");
            Matcher matcher = MIRRORED_COUNT_PATTERN.matcher(output);
            if (matcher.find()) {
                return output(matcher.group(1));
            }
            return output(output.trim());
        };
    }

    private SubmodelElement[] output(String value) {
        return new SubmodelElement[] {
            new Property("MirroredPackets", value)
        };
    }
}

