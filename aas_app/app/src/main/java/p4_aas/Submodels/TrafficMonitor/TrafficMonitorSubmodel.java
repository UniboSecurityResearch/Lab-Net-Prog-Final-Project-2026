package p4_aas.Submodels.TrafficMonitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.valuetype.ValueType;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;

import p4_aas.Submodels.AbstractSubmodel;

public class TrafficMonitorSubmodel extends AbstractSubmodel {
    private final TrafficMonitorLambda lambdaProvider;

    public TrafficMonitorSubmodel() {
        super();
        this.lambdaProvider = new TrafficMonitorLambda();
    }

    @Override
    public List<Submodel> createSubmodel() {
        Submodel tm = new Submodel();
        tm.setIdShort("TrafficMonitor");

        Map<String, ValueType> inputs = new LinkedHashMap<>();
        inputs.put("FunctionCode", ValueType.Integer);
        inputs.put("Threshold", ValueType.Integer);

        Operation op = new Operation("ActivateEncryption");
        op.setInputVariables(getUtils().getCustomInputVariables(inputs));
        op.setOutputVariables(getUtils().getOperationVariables(1, "Output"));
        op.setWrappedInvokable(lambdaProvider.activateEncryption());
        tm.addSubmodelElement(op);

        return List.of(tm);
    }
}
