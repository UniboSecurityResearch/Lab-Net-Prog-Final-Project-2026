package p4_aas.Submodels.ModbusMonitor;

import java.util.function.Supplier;

import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.vab.modelprovider.lambda.VABLambdaProviderHelper;

public class ReadOnlyProperty extends Property {
    private static final String VALUE_KEY = "value";
    private final Supplier<Object> getter;
    private boolean initialized = false;

    ReadOnlyProperty(Supplier<Object> getter) {
        this.getter = getter;
        super.put(VALUE_KEY, VABLambdaProviderHelper.createSimple(getter, null));
        this.initialized = true;
    }

    /**
     * Override put to block any write to "value" after initialization.
     */
    @Override
    public Object put(String key, Object value) {
        // Before initialization: allow everything (superclass constructor calls)
        if (!initialized) {
            return super.put(key, value);
        }
        // After initialization: block any write to "value"
        if (VALUE_KEY.equals(key)) {
            return null; // silently ignore
        }
        return super.put(key, value);
    }

    @Override
    public Object getValue() {
        return getter.get();
    }
}