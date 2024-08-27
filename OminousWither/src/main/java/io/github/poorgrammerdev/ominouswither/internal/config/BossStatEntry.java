package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class BossStatEntry implements ConfigurationSerializable {

    private final String formula;
    private final double[] levelMapping;
    private final double[] difficultyMapping;

    /**
     * Deserialize method with stat name for clearer debug messages
     * @param fields fields to deserialize from
     * @param statName name to use in exception message for easier debugging
     * @throws IllegalArgumentException invalid, missing, or malformed fields
     */
    public BossStatEntry(final Map<String, Object> fields, final String statName) throws IllegalArgumentException {
        //Read in the formula
        final Object formula = fields.getOrDefault("formula", null);
        if (formula == null) throw new IllegalArgumentException("Boss Stat " + statName + " is missing its formula field");
        if (!(formula instanceof String)) throw new IllegalArgumentException("Boss Stat " + statName + " has the wrong type for the formula field. Expected " + String.class + ", got " + formula.getClass());
        this.formula = (String) formula;

        //Read in level mapping if present, else set to null
        final Object levelMapping = fields.getOrDefault("level-mapping", null);
        this.levelMapping = (levelMapping != null) ? this.getMappingArray(levelMapping, "Level", 5, statName) : null;

        //Read in difficulty mapping if present, else set to null
        final Object difficultyMapping = fields.getOrDefault("difficulty-mapping", null);
        this.difficultyMapping = (difficultyMapping != null) ? this.getMappingArray(difficultyMapping, "Difficulty", 3, statName) : null;
    }

    /**
     * Required deserialize method without name -- will still function, just less helpful debug messages
     * @param fields fields to deserialize from
     * @throws IllegalArgumentException invalid, missing, or malformed fields
     */
    public BossStatEntry(final Map<String, Object> fields) throws IllegalArgumentException {
        this(fields, "STAT_NAME_NOT_PROVIDED");
    }

    @Override
    public Map<String, Object> serialize() {
        final HashMap<String, Object> ret = new HashMap<>();

        ret.put("formula", this.formula);
        if (this.levelMapping != null) ret.put("level-mapping", this.levelMapping);
        if (this.difficultyMapping != null) ret.put("difficulty-mapping", this.difficultyMapping);

        return ret;
    }

    // Getters 
    public String getFormula() {return this.formula;}
    public double[] getLevelMapping() {return this.levelMapping;}
    public double[] getDifficultyMapping() {return this.difficultyMapping;}

    /**
     * Turns a mapping entry into a double array
     * @param mapping mapping entry
     * @param metricName name of this value to use in exception message
     * @param length required list length to match
     * @param statName name of stat to use in exception message
     * @return the mapping (array of doubles)
     */
    private double[] getMappingArray(final Object mapping, final String metricName, final int length, final String statName) {
        //Mapping list must be a list
        if (!(mapping instanceof List<?>)) throw new IllegalArgumentException(metricName + " Mapping of Boss Stat " + statName + " must be a list");

        //List must be the exact accepted length
        final List<?> list = (List<?>) mapping;
        if (list.size() != length) throw new IllegalArgumentException(metricName + " Mapping list of Boss Stat " + statName + " must have exactly " + length + " values");

        //List must be all numeric
        if (!list.stream().allMatch(Number.class::isInstance)) throw new IllegalArgumentException(metricName + " Mappings of Boss Stat " + statName + " must be all numeric");

        @SuppressWarnings("unchecked")
        final List<? extends Number> numList = (List<? extends Number>) list;
        return numList.stream().mapToDouble(Number::doubleValue).toArray();
    }

    
}
