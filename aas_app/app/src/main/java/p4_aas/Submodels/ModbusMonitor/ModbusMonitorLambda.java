package p4_aas.Submodels.ModbusMonitor;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

import p4_aas.Submodels.SwitchRuntime.SwitchCliClient;

public class ModbusMonitorLambda {
    private static final int ALARM_THRESHOLD = 20;
    private static final Pattern REGISTER_VALUE = Pattern.compile("=\\s*(\\d+)");

    private final SwitchCliClient switchCliClient;

    public ModbusMonitorLambda() {
        this.switchCliClient = new SwitchCliClient();
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> getFc4Count() {
        return (args) -> {
            int count = readFc4Count();
            return new SubmodelElement[] { new Property("Fc4PacketCount", count) };
        };
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> getFc4Alarm() {
        return (args) -> {
            int count = readFc4Count();
            boolean alarm = count > ALARM_THRESHOLD;
            return new SubmodelElement[] {
                new Property("Fc4PacketCount", count),
                new Property("Fc4Alarm", alarm)
            };
        };
    }

    private int readFc4Count() {
        // s1 is the switch that does the counting (closest to the client)
        String raw = switchCliClient.runCliCommand(1, "register_read modbus_fc_counter 4");
        Matcher m = REGISTER_VALUE.matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}