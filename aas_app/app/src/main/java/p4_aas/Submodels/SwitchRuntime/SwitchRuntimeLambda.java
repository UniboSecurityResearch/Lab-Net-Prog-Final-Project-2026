package p4_aas.Submodels.SwitchRuntime;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwitchRuntimeLambda {
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_.]+";
    private final SwitchCliClient switchCliClient;

    public SwitchRuntimeLambda() {
        this.switchCliClient = new SwitchCliClient();
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> showTables() {
        return (args) -> output(switchCliClient.runCliCommand(getInt(args, "Switch"), "show_tables"));
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> dumpTable() {
        return (args) -> {
            String table = getIdentifier(args, "Table");
            if (table == null) {
                return output("Invalid table name");
            }
            return output(switchCliClient.runCliCommand(getInt(args, "Switch"), "table_dump " + table));
        };
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> readRegister() {
        return (args) -> {
            String register = getIdentifier(args, "Register");
            if (register == null) {
                return output("Invalid register name");
            }
            return output(switchCliClient.runCliCommand(getInt(args, "Switch"), "register_read " + register));
        };
    }

    private SubmodelElement[] output(String value) {
        return new SubmodelElement[] {
            new Property("Output", value)
        };
    }

    private String getIdentifier(Map<String, SubmodelElement> args, String name) {
        Object value = args.get(name).getValue();
        String identifier = String.valueOf(value);
        if (!identifier.matches(IDENTIFIER_PATTERN)) {
            return null;
        }
        return identifier;
    }

    private int getInt(Map<String, SubmodelElement> args, String name) {
        Object value = args.get(name).getValue();
        if (value instanceof BigInteger) {
            return ((BigInteger) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String removeRule(int switchId, String table_name, int functionCode) {
        String dumpOutput = switchCliClient.runCliCommand(switchId, "table_dump " + table_name);
        String targetHex = String.format("%02d", functionCode);
        
        String currentHexHandle = null;

        for (String line : dumpOutput.split("\n")) {
            line = line.trim();

            if (line.startsWith("Dumping entry")) {
                currentHexHandle = line.replace("Dumping entry", "").trim();
            }

            if (currentHexHandle != null && line.contains("modbus_pdu.function_code: EXACT")) {
                if (line.contains(targetHex) || line.contains(String.valueOf(functionCode))) {
                    String currentIntHandle = String.valueOf(Integer.decode(currentHexHandle));
                    String deleteResult = switchCliClient.runCliCommand(switchId, "table_delete " + table_name + " " + currentIntHandle);
                    return !deleteResult.contains("Error") && !deleteResult.contains("Invalid") ? "deleted" : ("error: " + deleteResult);
                }
            }
        }

        return "not_found"; 
    }

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> toggleEncryptionRule() {
        return (args) -> {
            Integer functionCode = getInt(args, "FunctionCode");
            if (functionCode == null || functionCode < 1 || functionCode > 6) {
                return output("Error: Function Code must be an integer between 1 and 6.");
            }

            String isRemoved1 = removeRule(1, "modbus_sec", functionCode);
            String isRemoved2 = removeRule(2, "modbus_sec", functionCode);

            if (isRemoved1.startsWith("error:") || isRemoved2.startsWith("error:")) {
                return output("Failed to remove existing rule from data plane." + " Output S1: " + isRemoved1 + " Output S2: " + isRemoved2);
            }

            if (isRemoved1.equals("deleted") || isRemoved2.equals("deleted")) {
                return output("Encrypted tunnel disabled for Function Code: " + functionCode);
            }

            String outputS1 = switchCliClient.runCliCommand(1, "table_add modbus_sec toggle_cipher " + functionCode + " => 2");
            String outputS2 = switchCliClient.runCliCommand(2, "table_add modbus_sec toggle_cipher " + functionCode + " => 2");
            
            if (outputS1.contains("Error") || outputS2.contains("Error")) {
                return output("Failed to apply rule to data plane." + "Output S1: " + outputS1 + " Output S2: " + outputS2);
            }
            return output("Encrypted tunnel enabled for Function Code: " + functionCode);
        };
    }
}
