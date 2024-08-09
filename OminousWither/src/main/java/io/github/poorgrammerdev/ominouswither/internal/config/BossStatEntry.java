package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class BossStatEntry implements ConfigurationSerializable {

    private final String formula;
    private final double[] levelMapping;
    private final double[] difficultyMapping;

    public BossStatEntry(final Map<String, Object> fields) throws IllegalArgumentException {
        //Read in the formula
        final Object formula = fields.getOrDefault("formula", null);
        if (formula == null) throw new IllegalArgumentException("A Boss Stat is missing its formula field");
        if (!(formula instanceof String)) throw new IllegalArgumentException("A Boss Stat's formula field is the wrong type. Expected " + String.class + ", got " + formula.getClass());
        this.formula = (String) formula;

        //Read in level mapping if present, else set to null
        final Object levelMapping = fields.getOrDefault("level-mapping", null);
        this.levelMapping = (levelMapping != null) ? this.getMappingArray(levelMapping, "Level", 5) : null;

        //Read in difficulty mapping if present, else set to null
        final Object difficultyMapping = fields.getOrDefault("difficulty-mapping", null);
        this.difficultyMapping = (difficultyMapping != null) ? this.getMappingArray(difficultyMapping, "Difficulty", 3) : null;
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
     * @param name name of this value to use in exception message
     * @param length required list length to match
     * @return the mapping (array of doubles)
     */
    private double[] getMappingArray(final Object mapping, final String name, final int length) {
        //Mapping list must be a list
        if (!(mapping instanceof List<?>)) throw new IllegalArgumentException("Boss Stat " + name + " Mapping must be a list");

        //List must be the exact accepted length
        final List<?> list = (List<?>) mapping;
        if (list.size() != length) throw new IllegalArgumentException("Boss Stat " + name + " Mapping list must have exactly " + length + " values");

        //List must be all numeric
        if (!list.stream().allMatch(Number.class::isInstance)) throw new IllegalArgumentException("Boss Stat " + name + " Mappings must be all numeric");

        @SuppressWarnings("unchecked")
        final List<? extends Number> numList = (List<? extends Number>) list;
        return numList.stream().mapToDouble(Number::doubleValue).toArray();
    }

    
}
