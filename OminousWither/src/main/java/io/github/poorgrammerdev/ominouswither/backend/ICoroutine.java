package io.github.poorgrammerdev.ominouswither.backend;

/**
 * Represents a task that's split across multiple ticks 
 * Referencing code from this thread: https://www.spigotmc.org/threads/guide-on-workload-distribution-or-how-to-handle-heavy-splittable-tasks.409003/
 */
public interface ICoroutine {
    /**
     * Run a single operation of the task
     */
    public void tick();

    /**
     * Should this task be put back into the queue?
     * @return if the task should be rescheduled to run again next tick
     */
    public boolean shouldBeRescheduled();
}
