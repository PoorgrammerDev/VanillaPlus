package io.github.poorgrammerdev.ominouswither.internal.config;

import org.bukkit.Difficulty;

import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;

/**
 * Holds the values for a single configurable Boss setting
 * @author Thomas Tran
 */
public class CachedBossSetting {
    private static final int TABLE_WIDTH = 5;

    private boolean isLevelDependent;
    private boolean isDifficultyDependent;
    private final double[] data;

    public CachedBossSetting(final BossStatEntry entry, final EvaluationEnvironment evalEnv) {
        //Determine if the formula depends on the level or difficulty variables
        this.isLevelDependent = entry.getFormula().contains("level");
        this.isDifficultyDependent = entry.getFormula().contains("difficulty");

        //Create the data array based on that information
        this.data = new double[this.getDataSize()];

        //Compile the given expression, then evaluate and cache results for all possible values of level and difficulty
        final CompiledExpression expression = Crunch.compileExpression(entry.getFormula(), evalEnv);
        for (int levelIndex = 0; levelIndex < (this.isLevelDependent ? 5 : 1); ++levelIndex) {
            for (int difficultyIndex = 0; difficultyIndex < (this.isDifficultyDependent ? 3 : 1); ++difficultyIndex) {

                //Apply manual mappings if present, otherwise transform indices to values {[0,(max-1)] range => [1,max] range}
                final double level = entry.getLevelMapping() != null ? entry.getLevelMapping()[levelIndex] : levelIndex + 1;
                final double difficulty = entry.getDifficultyMapping() != null ? entry.getDifficultyMapping()[difficultyIndex] : difficultyIndex + 1;

                this.data[this.getDataIndex(levelIndex, difficultyIndex)] = expression.evaluate(level, difficulty);
            }
        }
    }

    public double getValue(int level, final Difficulty difficulty) {
        return this.data[this.getDataIndex(level - 1, this.getDifficultyIndex(difficulty))];
    }

    private int getDataSize() {
        return 1 * (this.isLevelDependent ? 5 : 1) * (this.isDifficultyDependent ? 3 : 1);
    }

    private int getDataIndex(int levelIndex, int difficultyIndex) {
        if (this.isLevelDependent && this.isDifficultyDependent) {
            return (levelIndex + (TABLE_WIDTH * difficultyIndex));
        }
        else if (this.isLevelDependent) {
            return levelIndex;
        }
        else if (this.isDifficultyDependent) {
            return difficultyIndex;
        }

        return 0;
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
