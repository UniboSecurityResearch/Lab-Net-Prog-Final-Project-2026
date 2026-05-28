package p4_aas.Submodels.SwitchRuntime;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.*;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

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

     public Function<Map<String, SubmodelElement>, SubmodelElement[]> readFunctionCodeRegisterBySwitch() {
        return (args) -> {
            int functionCode = getInt(args, "FunctionCode");
            if (functionCode < 1 || functionCode > 6) {
                return output("Invalid function code");
            }
            int switchNum = getInt(args, "Switch");

            String registerName = "function_code_counters";
            String outS1 = switchCliClient.runCliCommand(switchNum, "register_read " + registerName + " " + functionCode);
            //String outS2 = switchCliClient.runCliCommand(2, "register_read " + registerName + " " + functionCode);
            int count = Integer.parseInt(extractValue(outS1)); //+ Integer.parseInt(extractValue(outS2));
            return output("" + count);
        };
    }

     public Function<Map<String, SubmodelElement>, SubmodelElement[]> readFunctionCodeRegister() {
        return (args) -> {
            int functionCode = getInt(args, "FunctionCode");
            if (functionCode < 1 || functionCode > 6) {
                return output("Invalid function code");
            }
            String registerName = "function_code_counters";
            String outS1 = switchCliClient.runCliCommand(1, "register_read " + registerName + " " + functionCode);
            String outS2 = switchCliClient.runCliCommand(2, "register_read " + registerName + " " + functionCode);
            int count = Integer.parseInt(extractValue(outS1)) + Integer.parseInt(extractValue(outS2));
            return output("" + count);
        };
    }

    private String extractValue(String output){
        Pattern p = Pattern.compile("=\\s*(\\d+)");
        Matcher m = p.matcher(output);
        if (m.find()) {
            return m.group(1);
        } else {
            return "Error in extracting count";
        }
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
}
