package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.List;

import org.bukkit.Difficulty;

/**
 * Holds the values for a single configurable Boss setting
 * @author Thomas Tran
 */
public class CachedBossSetting {
    private static final int TABLE_WIDTH = 5;

    private final MetricDependencyType dependencyType;
    private final double[] data;

    //TODO: this whole class is quite messy and probably needs some sort of rewrite/refactor

    public CachedBossSetting(final List<BossModifier> modifiers) {
        boolean isLevelDependent = false;
        boolean isDifficultyDependent = false;

        //Loop through defined rules and update values above
        for (final BossModifier modifier : modifiers) {
            if (isLevelDependent && isDifficultyDependent) break;

            if (!isLevelDependent && modifier.getType().equals("level")) isLevelDependent = true;
            if (!isDifficultyDependent && modifier.getType().equals("difficulty")) isDifficultyDependent = true;
        }

        //Translate booleans to enum
        if (isLevelDependent && isDifficultyDependent) this.dependencyType = MetricDependencyType.BOTH_LEVEL_AND_DIFFICULTY;
        else if (isLevelDependent) this.dependencyType = MetricDependencyType.LEVEL_ONLY;
        else if (isDifficultyDependent) this.dependencyType = MetricDependencyType.DIFFICULTY_ONLY;
        else this.dependencyType = MetricDependencyType.NONE;

        this.data = new double[this.dependencyType.getDataSize()];

        switch (this.dependencyType) {
            case BOTH_LEVEL_AND_DIFFICULTY:
                for (int level = 0; level <= 4; ++level) {
                    for (int difficulty = 0; difficulty <= 2; ++difficulty) {
                        this.data[level + (TABLE_WIDTH * difficulty)] = this.calculate(modifiers, level, difficulty);
                    }
                }
                break;
            case LEVEL_ONLY:
                for (int level = 0; level <= 4; ++level) {
                    this.data[level] = this.calculate(modifiers, level, 0);
                }
                break;
            case DIFFICULTY_ONLY:
                for (int difficulty = 0; difficulty <= 2; ++difficulty) {
                    this.data[difficulty] = this.calculate(modifiers, 0, difficulty);
                }
                break;
            case NONE:
                this.data[0] = this.calculate(modifiers, 0, 0);
                break;
            default:
                throw new IllegalStateException("Dependency Type not set or invalid");

        }

    }

    public double getValue(int level, final Difficulty difficulty) {
        --level; //Levels in-game are on a [1-5] scale; we need a [0-4] scale for indexing
        final int difficultyIndex = this.getDifficultyIndex(difficulty);

        switch (this.dependencyType) {
            case BOTH_LEVEL_AND_DIFFICULTY:
                return this.data[level + (TABLE_WIDTH * difficultyIndex)];
            case LEVEL_ONLY:
                return this.data[level];
            case DIFFICULTY_ONLY:
                return this.data[difficultyIndex];
            case NONE:
                return this.data[0];
            default:
                throw new IllegalStateException("Dependency Type not set or invalid");

        }

    }

    private double calculate(final List<BossModifier> modifiers, final int levelIndex, final int difficultyIndex) {
        double ret = 0.0D;

        //Level calculations are done in a [1-5] scale but indexing is [0-4] scale, so add 1
        final int level = levelIndex + 1;

        //Difficulty calculations are done in [1-3] scale but indexing is [0-2] scale, so add 1
        final int difficulty = difficultyIndex + 1;

        for (final BossModifier modifier : modifiers) {
            switch (modifier.getType()) {
                case "level":
                    ret = this.applyModifier(modifier.getMode(), ret, modifier.getValue(), level);
                    break;
                case "difficulty":
                    ret = this.applyModifier(modifier.getMode(), ret, modifier.getValue(), difficulty);
                    break;
                case "base":
                    ret = this.applyModifier(modifier.getMode(), ret, modifier.getValue(), 1);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid modifier type " + modifier.getType());
            }
        }

        return ret;
    }

    private double applyModifier(final String mode, final double accumulatingValue, final double modifierValue, final int metric) {
        switch (mode) {
            case "additive":
                return accumulatingValue + (modifierValue * metric);
            case "multiplicative":
                return accumulatingValue * (modifierValue * metric);
            default:
                throw new IllegalArgumentException("Invalid modifier mode " + mode);
        }
    }

    private int getDifficultyIndex(final Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 0;
            case NORMAL:
                return 1;
            case HARD:
                return 2;

            // Invalid cases -> just assume Easy mode
            case PEACEFUL:
                return 0;
            default:
                return 0;
            
        }
    }

}
