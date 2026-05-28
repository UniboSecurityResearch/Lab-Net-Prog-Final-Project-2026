# Challenge 3 Project Report

## 1. Introduction

This report describes our implementation of Challenge #3, whose goal is to monitor Modbus traffic in an industrial network and expose the collected information through the Asset Administration Shell (AAS) infrastructure.

The provided network is composed of two hosts, two P4 switches, and the AAS management infrastructure. In this scenario, the first host represents a working machine acting as a Modbus client, while the second host represents a plant controller acting as a Modbus server. The client sends Modbus requests to the server, and the server replies with the requested data or operation result.

The challenge requires counting all Modbus packets related to function codes from `1` to `6`. The packet counting must be implemented in P4, while the resulting counter values must be exposed through a new AAS submodel under the Network Infrastructure AAS.

## 2. Objectives

The work had three main objectives:

1. Extend the P4 switch program so that Modbus packets are classified by function code.
2. Store the number of packets for function codes `1` to `6` since switch startup.
3. Add a new AAS submodel that allows the user to read those counters from the management infrastructure.

The Modbus function codes considered in the implementation are:

- `1`: Read Coils
- `2`: Read Discrete Inputs
- `3`: Read Holding Registers
- `4`: Read Input Registers
- `5`: Write Single Coil
- `6`: Write Single Register

## 3. Initial Environment Analysis

Before implementing the challenge, we inspected the provided environment to understand the existing structure and reuse the components already available in the project.

The relevant files were:

- `s1/industrial_aes.p4`
- `s2/industrial_aes.p4`
- `aas_app/`
- `aas_project/P4_AAS.jar`
- `aas_app/app/src/main/java/p4_aas/Submodels/SwitchRuntime/`

The two P4 programs define the behavior of the two switches. The Java code under `aas_app/` defines the BaSyx-based AAS application. The existing switch runtime submodel already provided a mechanism for executing `simple_switch_CLI` commands on the switches through SSH. We reused this mechanism to avoid introducing a separate management service.

The traffic path in the topology is:

```text
modbusclient -> s1 -> s2 -> modbusserver
modbusserver -> s2 -> s1 -> modbusclient
```

Since each Modbus operation normally generates one request packet and one response packet, a successful operation can increase the counter of the related function code by two packets when observed on one switch.

## 4. P4 Dataplane Implementation

The packet counting logic was implemented in both P4 programs:

- `s1/industrial_aes.p4`
- `s2/industrial_aes.p4`

Both switches use the same logic so that either switch can be used as an observation point.

### 4.1 Extracting the Modbus Function Code

The existing parser already detected Modbus TCP traffic by checking TCP port `502`. We extended this parser so that, after the Modbus TCP header is extracted, the first byte of the Modbus payload is read.

In Modbus, the first byte of the Protocol Data Unit (PDU) is the function code. To capture it without changing the existing payload extraction logic, we used:

```p4
packet.lookahead<bit<8>>()
```

The extracted value is stored in parser metadata:

```p4
bit<1> has_modbus_function_code;
bit<8> modbus_function_code;
```

The first field indicates whether a function code was successfully captured. The second field stores the function code itself.

This design was chosen because the original P4 program already used the payload for encryption and decryption. Reading the first payload byte through metadata allowed us to add monitoring without changing the existing payload layout or the cryptographic processing path.

### 4.2 Register Used for Packet Counters

We added a BMv2 register with six entries:

```p4
register<bit<32>>(6) modbus_function_code_packet_counts;
```

The register layout is:

- index `0`: packets related to FC1
- index `1`: packets related to FC2
- index `2`: packets related to FC3
- index `3`: packets related to FC4
- index `4`: packets related to FC5
- index `5`: packets related to FC6

The values stored in this register are the authoritative packet counters. They are maintained inside the switch dataplane and persist until the switch is restarted.

### 4.3 Counting Action

The P4 ingress pipeline was extended with an action called:

```p4
count_modbus_function_code_packet()
```

This action checks whether a Modbus function code was captured. If the code is between `1` and `6`, it maps the function code to the corresponding register index and increments the stored value.

The implementation also handles Modbus exception responses. In Modbus, exception responses are encoded by setting the highest bit of the original function code. Therefore, exception responses for function codes `1` to `6` appear as values `129` to `134`. We map these values back to the same six counters so that exception packets are still counted as packets related to the corresponding function code.

The counting action is called only for valid Modbus TCP packets:

```p4
if (hdr.tcp.isValid()) {
    if (hdr.modbus_tcp.isValid()) {
        count_modbus_function_code_packet();
        modbus_sec.apply();
    }
}
```

This ensures that unrelated traffic does not affect the counters.

## 5. AAS Submodel Implementation

The second part of the challenge was to expose the P4 counters through the AAS infrastructure. We added a new submodel called:

```text
ModbusPacketCounters
```

This submodel is attached to the Network Infrastructure AAS.

### 5.1 Network Infrastructure Integration

The file:

```text
aas_app/app/src/main/java/p4_aas/AssetShells/NetworkInfrastructure.java
```

was modified so that the Network Infrastructure shell includes the new submodel in addition to the existing topology submodel.

The Network Infrastructure AAS therefore exposes:

- `NetworkTopology`
- `ModbusPacketCounters`

### 5.2 Submodel Structure

The file:

```text
aas_app/app/src/main/java/p4_aas/Submodels/NetworkInfrastructure/ModbusPacketCountersSubmodel.java
```

defines the AAS-facing structure of the new submodel.

It includes descriptive properties:

- `CountedFunctionCodes`
- `DefaultObservationSwitch`
- `CountingNotes`

It also defines the operation:

```text
ReadCounters
```

The operation takes one input:

- `Switch`

The operation returns:

- `ObservationSwitch`
- `FC1Count`
- `FC2Count`
- `FC3Count`
- `FC4Count`
- `FC5Count`
- `FC6Count`

These six fields correspond to the six entries of the P4 register.

### 5.3 Runtime Logic

The runtime behavior of the `ReadCounters` operation is implemented in:

```text
aas_app/app/src/main/java/p4_aas/Submodels/NetworkInfrastructure/ModbusPacketCountersLambda.java
```

When the operation is invoked, the Java code:

1. reads the selected switch identifier
2. sends a register-read command to the switch
3. receives the textual output from BMv2
4. parses the six counter values
5. returns them as AAS output properties

The register that is read is:

```text
modbus_function_code_packet_counts
```

## 6. Use of `simple_switch_CLI`

The packet counting itself is performed entirely in the P4 dataplane. The AAS application does not count packets and does not maintain a separate copy of the counters.

However, the counters are stored inside the running BMv2 switch process. The Java AAS application cannot directly access this internal memory as normal Java state. It requires a runtime interface to query the switch.

For this reason, we used the runtime interface already available in the provided environment:

```text
simple_switch_CLI
```

The command executed by the AAS layer is:

```bash
echo "register_read modbus_function_code_packet_counts" | simple_switch_CLI
```

This command is executed on the selected switch through the existing `SwitchCliClient` and SSH management path.

This design keeps the P4 register as the single source of truth. The CLI is used only to retrieve the live register values and expose them through the AAS.

In principle, the same operation could also be implemented through a programmatic API such as P4Runtime or the BMv2 Thrift API. We used `simple_switch_CLI` because the provided project already included this management mechanism, and it allowed us to integrate the new submodel without adding another runtime client stack.

## 7. Interpretation of Switch Counters

Both switches run the same P4 program and both observe the Modbus traffic crossing the network. Therefore, it is possible for `s1` and `s2` to report the same counter values.

This behavior is expected in the current implementation.

For network-wide interpretation, the counters from the two switches should not be summed, because this would count the same packets twice. Instead, one switch should be selected as the observation point. The second switch can still be queried for debugging or comparison.

## 8. Verification Procedure

The implementation can be verified using both the switch CLI and the AAS interface.

### 8.1 Generating Modbus Traffic from the AAS UI

To increase the counters from the Web UI, we use the operations exposed by the `ModbusClient` AAS. Each operation corresponds to one Modbus function code and therefore increases the related counter.

The mapping is:

- `ReadCoils` increases `FC1Count`
- `ReadDiscreteInputs` increases `FC2Count`
- `ReadHoldingRegisters` increases `FC3Count`
- `ReadInputRegisters` increases `FC4Count`
- `WriteCoil` increases `FC5Count`
- `WriteRegister` increases `FC6Count`

For the read operations, simple test inputs such as `Address = 0` and `Count = 1` are sufficient. For the write operations, example values are `Address = 0, Value = true` for `WriteCoil`, and `Register = 0, Value = 45` for `WriteRegister`.

### 8.2 Direct Switch Verification

After generating Modbus traffic, the counter register can be read directly from a switch using:

```bash
echo "register_read modbus_function_code_packet_counts" | simple_switch_CLI
```

The expected output contains six values, one for each Modbus function code.

### 8.3 AAS Verification

The same values can be read through the AAS by invoking the operation:

```text
ModbusPacketCounters.ReadCounters
```

with input:

```text
Switch = 1
```

The values returned by the AAS operation should match the values read directly from the BMv2 register on the selected switch.

### 8.4 Expected Result

After one successful request for each function code from `1` to `6`, the selected switch is expected to count both the request and the response packets. Therefore, each related counter is expected to increase by `2`.

For example:

```text
FC1Count = 2
FC2Count = 2
FC3Count = 2
FC4Count = 2
FC5Count = 2
FC6Count = 2
```

The exact values depend on how many Modbus operations were executed since the switch startup.

## 9. Files Modified

The main files modified for the challenge were:

- `s1/industrial_aes.p4`
- `s2/industrial_aes.p4`
- `aas_app/app/src/main/java/p4_aas/AssetShells/NetworkInfrastructure.java`
- `aas_app/app/src/main/java/p4_aas/Submodels/NetworkInfrastructure/ModbusPacketCountersSubmodel.java`
- `aas_app/app/src/main/java/p4_aas/Submodels/NetworkInfrastructure/ModbusPacketCountersLambda.java`
- `aas_app/app/src/main/java/p4_aas/Submodels/Utils/Utils.java`

The P4 files implement the actual packet counting. The Java files expose the collected values through the AAS infrastructure.

## 10. Conclusion

We implemented Modbus function-code monitoring by extending the P4 programs running on the two switches. The switches now count packets related to Modbus function codes `1` to `6` in a BMv2 register.

We also extended the Network Infrastructure AAS with a new `ModbusPacketCounters` submodel. This submodel provides a `ReadCounters` operation that reads the live register values from the selected switch and returns the six function-code counters to the user.

The final design keeps the monitoring logic in the dataplane, as required by the challenge, while using the existing AAS management infrastructure to present the results.
