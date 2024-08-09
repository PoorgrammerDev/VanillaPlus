package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

/**
 * Represents a modifier of a Boss Setting field in the config
 * @author Thomas Tran
 */
public class BossModifier implements ConfigurationSerializable{
    private final String type;
    private final double value;
    private final String mode;

    public BossModifier(String type, double value, String mode) {
        this.type = type;
        this.value = value;
        this.mode = mode;
    }

    public BossModifier(final Map<String, Object> fields) throws IllegalArgumentException {
        //TODO: this is quite messy and probably needs some sort of rewrite/refactor
        this.type = (String) fields.get("type");
        
        final Object val = fields.get("value");
        if (val instanceof Double) {
            this.value = (Double) val;
        }
        else if (val instanceof Float) {
            this.value = (Float) val;
        }
        else if (val instanceof Integer) {
            this.value = (Integer) val;
        }
        else throw new IllegalArgumentException("Value must be numeric");

        if (fields.containsKey("mode")) {
            this.mode = (String) fields.get("mode");
        }
        else {
            this.mode = "additive";
        }

    }

    @Override
    public Map<String, Object> serialize() {
        final HashMap<String, Object> ret = new HashMap<>();
        ret.put("type", this.type);
        ret.put("value", this.value);

        return ret;
    }

    public String getType() {
        return this.type;
    }

    public double getValue() {
        return this.value;
    }

    public String getMode() {
        return mode;
    }
}
