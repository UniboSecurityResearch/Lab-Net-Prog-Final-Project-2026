package p4_aas.Submodels.TrafficMonitor;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

import p4_aas.Submodels.SwitchRuntime.SwitchCliClient;

public class TrafficMonitorLambda {
    private final SwitchCliClient switchCliClient;

    public TrafficMonitorLambda() {
        this.switchCliClient = new SwitchCliClient();
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> activateEncryption() {
        return (args) -> {
            int fc = getInt(args, "FunctionCode");
            int threshold = getInt(args, "Threshold");
            if (fc < 1 || fc > 6) return output("Invalid function code (must be 1-6)");

            /*
            String raw = switchCliClient.runCliCommand(1, "register_read fc_counters " + fc);
            int count = parseRegisterValue(raw);
            if (count < threshold) {
                try {
                  switchCliClient.runCliCommand(1, "table_clear modbus_sec");
                  switchCliClient.runCliCommand(2, "table_clear modbus_sec");
                } catch (Exception e){
                    return output("OK. Threshold not met. threshold=" + threshold);
                }
                return output("OK. Threshold not met. threshold=" + threshold);
            } */
            
            switchCliClient.runCliCommand(1, "table_add modbus_sec decipher 1 =>");
            switchCliClient.runCliCommand(1, "table_add modbus_sec cipher 2 =>");
            switchCliClient.runCliCommand(2, "table_add modbus_sec decipher 1 =>");
            switchCliClient.runCliCommand(2, "table_add modbus_sec cipher 2 =>");

            switchCliClient.runCliCommand(1, "register_write fc_thresholds " + fc + " " + threshold);
            switchCliClient.runCliCommand(2, "register_write fc_thresholds " + fc + " " + threshold);
            
            return output("Encryption activated. FC" + fc);
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

    private int parseRegisterValue(String raw) {
        try {
            Pattern pattern = Pattern.compile("=\\s*(\\d+)");
            Matcher matcher = pattern.matcher(raw);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            System.err.println("Errore di parsing: " + e.getMessage());
        }
        return 0;
    }
}
