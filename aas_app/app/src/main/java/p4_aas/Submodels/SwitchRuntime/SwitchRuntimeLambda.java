package p4_aas.Submodels.SwitchRuntime;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Dictionary;
import java.util.Hashtable;

record HandlePair(Integer cipherHandle, Integer decipherHandle) {}

public class SwitchRuntimeLambda {
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9_.]+";
    private final SwitchCliClient switchCliClient;
    private final Hashtable<Integer, Hashtable<Integer, HandlePair>> rulesHandles = new Hashtable<>(); // ONLY FOR SECOND IMPLEMENTATION

    private Hashtable<Integer, HandlePair> switchHandlePair = new Hashtable<>(); // ONLY FOR THIRD IMPLEMENTATION
    private final Hashtable<Integer, Hashtable<Integer, Integer>> functionCodeHandles = new Hashtable<>(); // ONLY FOR THIRD IMPLEMENTATION

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


    /* FIRST IMPLEMENTATION: with 1 table with 1 field as key and 1 field as action parameter */

    // private String removeRule(int switchId, String table_name, int functionCode) {
    //     String dumpOutput = switchCliClient.runCliCommand(switchId, "table_dump " + table_name);
    //     String targetHex = String.format("%02d", functionCode);
        
    //     String currentHexHandle = null;

    //     for (String line : dumpOutput.split("\n")) {
    //         line = line.trim();

    //         if (line.startsWith("Dumping entry")) {
    //             currentHexHandle = line.replace("Dumping entry", "").trim();
    //         }

    //         if (currentHexHandle != null && line.contains("modbus_pdu.function_code: EXACT")) {
    //             if (line.contains(targetHex) || line.contains(String.valueOf(functionCode))) {
    //                 String currentIntHandle = String.valueOf(Integer.decode(currentHexHandle));
    //                 String deleteResult = switchCliClient.runCliCommand(switchId, "table_delete " + table_name + " " + currentIntHandle);
    //                 return !deleteResult.contains("Error") && !deleteResult.contains("Invalid") ? "deleted" : ("error: " + deleteResult);
    //             }
    //         }
    //     }

    //     return "not_found"; 
    // }

    // public Function<Map<String, SubmodelElement>, SubmodelElement[]> toggleEncryptionRule() {
    //     return (args) -> {
    //         Integer functionCode = getInt(args, "FunctionCode");
    //         if (functionCode == null || functionCode < 1 || functionCode > 6) {
    //             return output("Error: Function Code must be an integer between 1 and 6.");
    //         }

    //         String isRemoved1 = removeRule(1, "modbus_sec", functionCode);
    //         String isRemoved2 = removeRule(2, "modbus_sec", functionCode);

    //         if (isRemoved1.startsWith("error:") || isRemoved2.startsWith("error:")) {
    //             return output("Failed to remove existing rule from data plane." + " Output S1: " + isRemoved1 + " Output S2: " + isRemoved2);
    //         }

    //         if (isRemoved1.equals("deleted") || isRemoved2.equals("deleted")) {
    //             return output("Encrypted tunnel disabled for Function Code: " + functionCode);
    //         }

    //         String outputS1 = switchCliClient.runCliCommand(1, "table_add modbus_sec toggle_cipher " + functionCode + " => 2");
    //         String outputS2 = switchCliClient.runCliCommand(2, "table_add modbus_sec toggle_cipher " + functionCode + " => 2");
            
    //         if (outputS1.contains("Error") || outputS2.contains("Error")) {
    //             return output("Failed to apply rule to data plane." + "Output S1: " + outputS1 + " Output S2: " + outputS2);
    //         }
    //         return output("Encrypted tunnel enabled for Function Code: " + functionCode);
    //     };
    // }

    /* SECOND IMPLEMENTATION: 1 table with composite key (2 fields) */

    // public Function<Map<String, SubmodelElement>, SubmodelElement[]> toggleEncryptionRule() {
    //     return (args) -> {
    //         Integer functionCode = getInt(args, "FunctionCode");
    //         if (functionCode == null || functionCode < 1 || functionCode > 6) {
    //             return output("Error: Function Code must be an integer between 1 and 6.");
    //         }

    //         //Check if rule is already present
    //         Boolean isRuleAlreadyPresentS1 = (rulesHandles.containsKey(1) && rulesHandles.get(1).containsKey(functionCode));
    //         Boolean isRuleAlreadyPresentS2 = (rulesHandles.containsKey(2) && rulesHandles.get(2).containsKey(functionCode));

    //         //If present, remove rule and corresponding handle from hashtable
    //         if (isRuleAlreadyPresentS1) {
    //             HandlePair handlesS1 = rulesHandles.get(1).get(functionCode);
    //             String isRemovedS1_cipher = switchCliClient.runCliCommand(1, "table_delete modbus_sec " + handlesS1.cipherHandle());
    //             String isRemovedS1_decipher = switchCliClient.runCliCommand(1, "table_delete modbus_sec " + handlesS1.decipherHandle());
    //             rulesHandles.get(1).remove(functionCode);
    //         }

    //         if (isRuleAlreadyPresentS2) {
    //             HandlePair handlesS2 = rulesHandles.get(2).get(functionCode);
    //             String isRemovedS2_cipher = switchCliClient.runCliCommand(2, "table_delete modbus_sec " + handlesS2.cipherHandle());
    //             String isRemovedS2_decipher = switchCliClient.runCliCommand(2, "table_delete modbus_sec " + handlesS2.decipherHandle());
    //             rulesHandles.get(2).remove(functionCode);
    //         }

    //         if (isRuleAlreadyPresentS1 || isRuleAlreadyPresentS2) {
    //             return output("Encrypted tunnel disabled for Function Code: " + functionCode);
    //         }

    //         // Adding cipher and decipher rules for both switches and storing their handles in the hashtable
    //         String outputS1_cipher = switchCliClient.runCliCommand(1, "table_add modbus_sec cipher " + functionCode + " 1 =>");
    //         String outputS1_decipher = switchCliClient.runCliCommand(1, "table_add modbus_sec decipher " + functionCode + " 2 =>");

    //         String outputS2_cipher = switchCliClient.runCliCommand(2, "table_add modbus_sec cipher " + functionCode + " 1 =>");
    //         String outputS2_decipher = switchCliClient.runCliCommand(2, "table_add modbus_sec decipher " + functionCode + " 2 =>");
            
    //         if (outputS1_cipher.contains("Error") || outputS1_decipher.contains("Error") || outputS2_cipher.contains("Error") || outputS2_decipher.contains("Error")) {
    //             return output("Failed to apply rule to data plane.");
    //         }

    //         Pattern pattern = Pattern.compile("Entry has been added with handle (\\d+)");
    //         Matcher matcherS1_cipher = pattern.matcher(outputS1_cipher);
    //         Matcher matcherS1_decipher = pattern.matcher(outputS1_decipher);
    //         Matcher matcherS2_cipher = pattern.matcher(outputS2_cipher);
    //         Matcher matcherS2_decipher = pattern.matcher(outputS2_decipher);

    //         if (matcherS1_cipher.find() && matcherS1_decipher.find() && matcherS2_cipher.find() && matcherS2_decipher.find()) {
    //             Integer handleS1_cipher = Integer.parseInt(matcherS1_cipher.group(1));
    //             Integer handleS1_decipher = Integer.parseInt(matcherS1_decipher.group(1));
    //             Integer handleS2_cipher = Integer.parseInt(matcherS2_cipher.group(1));
    //             Integer handleS2_decipher = Integer.parseInt(matcherS2_decipher.group(1));

    //             rulesHandles.putIfAbsent(1, new Hashtable<>());
    //             rulesHandles.putIfAbsent(2, new Hashtable<>());
        
    //             rulesHandles.get(1).put(functionCode, new HandlePair(handleS1_cipher, handleS1_decipher));
    //             rulesHandles.get(2).put(functionCode, new HandlePair(handleS2_cipher, handleS2_decipher));
    //         } else {
    //             return output("Failed to parse handles from CLI output." + "Output S1 Cipher: " + outputS1_cipher + " Output S1 Decipher: " + outputS1_decipher + " Output S2 Cipher: " + outputS2_cipher + " Output S2 Decipher: " + outputS2_decipher);
    //         }
    //         return output("Encrypted tunnel enabled for Function Code: " + functionCode);
    //     };
    // }

    /* THIRD IMPLEMENTATION: 2 table with a single key in each */

    public Function<Map<String, SubmodelElement>, SubmodelElement[]> toggleEncryptionRule() {
        return (args) -> {
            Integer functionCode = getInt(args, "FunctionCode");
            if (functionCode == null || functionCode < 1 || functionCode > 6) {
                return output("Error: Function Code must be an integer between 1 and 6.");
            }

            //Check if rule is already present
            Boolean isFunctionCodePresentS1 = (functionCodeHandles.containsKey(1) && functionCodeHandles.get(1).containsKey(functionCode));
            Boolean isFunctionCodePresentS2 = (functionCodeHandles.containsKey(2) && functionCodeHandles.get(2).containsKey(functionCode));

            Boolean isRuleAlreadyPresentS1 = switchHandlePair.containsKey(1);
            Boolean isRuleAlreadyPresentS2 = switchHandlePair.containsKey(2);

            //If present, remove function code rule and corresponding handle from hashtable
            if (isFunctionCodePresentS1) {
                Integer FunctionCodeHandleS1 = functionCodeHandles.get(1).get(functionCode);
                String isRemovedS1_functionCode = switchCliClient.runCliCommand(1, "table_delete modbus_sec " + FunctionCodeHandleS1);
                functionCodeHandles.get(1).remove(functionCode);
            }

            if (isFunctionCodePresentS2) {
                Integer FunctionCodeHandleS2 = functionCodeHandles.get(2).get(functionCode);
                String isRemovedS2_functionCode = switchCliClient.runCliCommand(2, "table_delete modbus_sec " + FunctionCodeHandleS2);
                functionCodeHandles.get(2).remove(functionCode);
            }

            if (isFunctionCodePresentS1 || isFunctionCodePresentS2) {
                return output("Encrypted tunnel disabled for Function Code: " + functionCode);
            }

            Pattern pattern = Pattern.compile("Entry has been added with handle (\\d+)");

            if (!isRuleAlreadyPresentS1) {
                String outputS1_cipher = switchCliClient.runCliCommand(1, "table_add modbus_port_sec cipher 1 =>");
                String outputS1_decipher = switchCliClient.runCliCommand(1, "table_add modbus_port_sec decipher 2 =>");

                if(outputS1_cipher.contains("Error") || outputS1_decipher.contains("Error")) {
                    return output("Failed to apply rule to data plane for table modbus_port_sec.");
                }

                Matcher matcherS1_cipher = pattern.matcher(outputS1_cipher);
                Matcher matcherS1_decipher = pattern.matcher(outputS1_decipher);

                if (matcherS1_cipher.find() && matcherS1_decipher.find()) {
                    Integer handleS1_cipher = Integer.parseInt(matcherS1_cipher.group(1));
                    Integer handleS1_decipher = Integer.parseInt(matcherS1_decipher.group(1));

                    switchHandlePair.put(1, new HandlePair(handleS1_cipher, handleS1_decipher));
                } else {
                    return output("Failed to parse handles from CLI output for switch 1.");
                }
            }

            if (!isRuleAlreadyPresentS2) {
                String outputS2_cipher = switchCliClient.runCliCommand(2, "table_add modbus_port_sec cipher 1 =>");
                String outputS2_decipher = switchCliClient.runCliCommand(2, "table_add modbus_port_sec decipher 2 =>");

                if(outputS2_cipher.contains("Error") || outputS2_decipher.contains("Error")) {
                    return output("Failed to apply rule to data plane for table modbus_port_sec.");
                }

                Matcher matcherS2_cipher = pattern.matcher(outputS2_cipher);
                Matcher matcherS2_decipher = pattern.matcher(outputS2_decipher);

                if (matcherS2_cipher.find() && matcherS2_decipher.find()) {
                    Integer handleS2_cipher = Integer.parseInt(matcherS2_cipher.group(1));
                    Integer handleS2_decipher = Integer.parseInt(matcherS2_decipher.group(1));

                    switchHandlePair.put(2, new HandlePair(handleS2_cipher, handleS2_decipher));
                } else {
                    return output("Failed to parse handles from CLI output for switch 2.");
                }
            }

            // Adding cipher and decipher rules for both switches and storing their handles in the hashtable
            String outputS1_function_code = switchCliClient.runCliCommand(1, "table_add modbus_sec set_need_crypto " + functionCode + " =>");
            String outputS2_function_code = switchCliClient.runCliCommand(2, "table_add modbus_sec set_need_crypto " + functionCode + " =>");
            
            if (outputS1_function_code.contains("Error") || outputS2_function_code.contains("Error")) {
                return output("Failed to apply rule to data plane for table modbus_sec.");
            }

            Matcher matcherS1_function_code = pattern.matcher(outputS1_function_code);
            Matcher matcherS2_function_code = pattern.matcher(outputS2_function_code);
            if (matcherS1_function_code.find() && matcherS2_function_code.find()) {

                Integer handleS1_function_code = Integer.parseInt(matcherS1_function_code.group(1));
                Integer handleS2_function_code = Integer.parseInt(matcherS2_function_code.group(1));

                functionCodeHandles.putIfAbsent(1, new Hashtable<>());
                functionCodeHandles.putIfAbsent(2, new Hashtable<>());
        
                functionCodeHandles.get(1).put(functionCode, handleS1_function_code);
                functionCodeHandles.get(2).put(functionCode, handleS2_function_code);
            } else {
                return output("Failed to parse handles from CLI output for table modbus_sec.");
            }
            return output("Encrypted tunnel enabled for Function Code: " + functionCode);
        };
    }
}
