package io.github.poorgrammerdev.ominouswither.internal.config;

/**
 * Holds what metrics a Boss Setting depends on, and as a result how many values need to be cached
 * @author Thomas Tran
 */
public enum MetricDependencyType {
    BOTH_LEVEL_AND_DIFFICULTY(15),
    LEVEL_ONLY(5),
    DIFFICULTY_ONLY(3),
    NONE(1),
    ;

    private final int dataSize;

    private MetricDependencyType(final int dataSize) {
        this.dataSize = dataSize;
    }

    public int getDataSize() {
        return this.dataSize;
    }
}
