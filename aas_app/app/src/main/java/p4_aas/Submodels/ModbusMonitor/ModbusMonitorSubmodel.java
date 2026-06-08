package p4_aas.Submodels.ModbusMonitor;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.qualifier.qualifiable.Qualifier;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.valuetype.ValueType;
import org.eclipse.basyx.vab.modelprovider.lambda.VABLambdaProviderHelper;

import p4_aas.Submodels.AbstractSubmodel;
import p4_aas.Submodels.SwitchRuntime.SwitchCliClient;

public class ModbusMonitorSubmodel extends AbstractSubmodel {
    private static final int ALARM_THRESHOLD = 20;
    private static final int FC4_REGISTER_INDEX = 4;
    private static final String FC4_REGISTER_NAME = "modbus_fc_counter";
    private static final int COUNTING_SWITCH_ID = 1;     // s1
    private static final long CACHE_TTL_MS = 250;
    private static final Pattern REGISTER_VALUE = Pattern.compile("=\\s*(\\d+)");

    private final SwitchCliClient switchCliClient;

    // simple time-bounded cache so the two property reads on one refresh
    // share a single SSH round-trip
    private long lastCachedAt = 0L;
    private int cachedCount = 0;

    public ModbusMonitorSubmodel() {
        super();
        this.switchCliClient = new SwitchCliClient();
    }

    @Override
    public List<Submodel> createSubmodel() {
        Submodel monitor = new Submodel();
        monitor.setIdShort("ModbusMonitor");

        monitor.addSubmodelElement(buildFc4CountProperty());
        monitor.addSubmodelElement(buildFc4AlarmProperty());

        return List.of(monitor);
    }

    private Property buildFc4CountProperty() {
        Supplier<Object> getter = this::readFc4Count;

        Property p = new ReadOnlyProperty(getter);
        p.setIdShort("Fc4PacketCount");
        p.setValueType(ValueType.Integer);
        markReadOnly(p);
        
        return p;
    }

    private Property buildFc4AlarmProperty() {
        Supplier<Object> getter = () -> readFc4Count() > ALARM_THRESHOLD;

        Property p = new ReadOnlyProperty(getter);
        p.setIdShort("Fc4Alarm");
        p.setValueType(ValueType.Boolean);
        markReadOnly(p);

        return p;
    }

    private void markReadOnly(Property property) {
        Qualifier readOnly = new Qualifier("ReadOnly", ValueType.Boolean);
        readOnly.setValue(true);
        property.setQualifiers(List.of(readOnly));
    }

    private synchronized int readFc4Count() {
        long now = System.currentTimeMillis();
        if (now - lastCachedAt > CACHE_TTL_MS) {
            String raw = switchCliClient.runCliCommand(
                COUNTING_SWITCH_ID,
                "register_read " + FC4_REGISTER_NAME + " " + FC4_REGISTER_INDEX
            );
            Matcher m = REGISTER_VALUE.matcher(raw);
            cachedCount = m.find() ? Integer.parseInt(m.group(1)) : -1;
            lastCachedAt = now;
        }
        return cachedCount;
    }
}