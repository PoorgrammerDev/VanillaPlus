package io.github.poorgrammerdev.ominouswither.internal;

/**
 * Represents a task that's split across multiple ticks 
 * Referencing code from this thread: https://www.spigotmc.org/threads/guide-on-workload-distribution-or-how-to-handle-heavy-splittable-tasks.409003/
 */
public interface ICoroutine {
    /**
     * Run a single operation of the task
     * @return if the task should be rescheduled (put back into the queue) to run again next tick
     */
    public boolean tick();

}
