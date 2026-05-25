package p4_aas.Submodels.NetworkInfrastructure;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.valuetype.ValueType;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import p4_aas.StaticProperties;
import p4_aas.Submodels.AbstractSubmodel;

public class NetworkInfrastructureSubmodel extends AbstractSubmodel {

    private final NetworkInfrastructureLambda lambdaProvider;

    public NetworkInfrastructureSubmodel() {
        super();
        this.lambdaProvider = new NetworkInfrastructureLambda();
    }

    @Override
    public List<Submodel> createSubmodel() {

        Submodel topology = new Submodel();
        topology.setIdShort("NetworkTopology");
        topology.addSubmodelElement(new Property("Switches", "s1, s2"));
        topology.addSubmodelElement(new Property("Links", "modbusclient-s1, s1-s2, s2-modbusserver"));
        topology.addSubmodelElement(new Property("ManagementNetwork", "100.0.1.0/24"));
        topology.addSubmodelElement(new Property("SwitchManagementIPs", "s1=" + StaticProperties.SW1_MANAGEMENT_IP + ", s2=" + StaticProperties.SW2_MANAGEMENT_IP));
        topology.addSubmodelElement(new Property("ModbusClient", StaticProperties.MODBUS_CLIENT_IP));
        topology.addSubmodelElement(new Property("ModbusServer", StaticProperties.MODBUS_SERVER_IP));
        topology.addSubmodelElement(new Property("ModbusPort", StaticProperties.MODBUS_PORT));
        topology.addSubmodelElement(new Property("AASNetworks", "L=100.0.2.4, D=100.0.1.5, A=195.11.14.100, C=200.1.1.100"));

      
        Submodel modbusPolicy = new Submodel();
        modbusPolicy.setIdShort("ModbusTrafficPolicy");
        modbusPolicy.addSubmodelElement(new Property("Description", "Monitors Modbus FC1 and blocks when packet count exceeds threshold"));
        modbusPolicy.addSubmodelElement(configureFc1RateLimit());
        modbusPolicy.addSubmodelElement(getFc1PacketCount());
        modbusPolicy.addSubmodelElement(resetFc1Counter());

        return List.of(topology, modbusPolicy);
    }

    private Operation configureFc1RateLimit() {
        Operation op = new Operation("ConfigureFc1RateLimit");
        Map<String, ValueType> inputs = new LinkedHashMap<>();
        inputs.put("Switch", ValueType.Integer);
        inputs.put("Threshold", ValueType.Integer);
        inputs.put("Enabled", ValueType.Integer);
        op.setInputVariables(getUtils().getCustomInputVariables(inputs));
        op.setOutputVariables(getUtils().getOperationVariables(1, "Output"));
        op.setWrappedInvokable(lambdaProvider.configureFc1RateLimit());
        return op;
    }

    private Operation getFc1PacketCount() {
        Operation op = new Operation("GetFc1PacketCount");
        op.setInputVariables(getUtils().getCustomInputVariables(Map.of("Switch", ValueType.Integer)));
        op.setOutputVariables(getUtils().getOperationVariables(1, "Output"));
        op.setWrappedInvokable(lambdaProvider.getFc1PacketCount());
        return op;
    }

    private Operation resetFc1Counter() {
        Operation op = new Operation("ResetFc1Counter");
        op.setInputVariables(getUtils().getCustomInputVariables(Map.of("Switch", ValueType.Integer)));
        op.setOutputVariables(getUtils().getOperationVariables(1, "Output"));
        op.setWrappedInvokable(lambdaProvider.resetFc1Counter());
        return op;
    }
}
