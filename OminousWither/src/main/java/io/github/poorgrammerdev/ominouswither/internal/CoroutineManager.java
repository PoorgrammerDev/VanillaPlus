package io.github.poorgrammerdev.ominouswither.internal;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.scheduler.BukkitRunnable;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * Manages running operations of tasks that have been split across multiple ticks with a CPU time limitation
 * Please note that these tasks are running on the Main thread; they are not async.
 * Referencing code from this thread: https://www.spigotmc.org/threads/guide-on-workload-distribution-or-how-to-handle-heavy-splittable-tasks.409003/
 */
public class CoroutineManager extends BukkitRunnable {
    private int maxNanosPerTick;

    public CoroutineManager() {
        //Set to a placeholder default value before the config is loaded in
        this.maxNanosPerTick = (int) (2.5D * 1E6);
    }

    public void load(final OminousWither plugin) {
        this.maxNanosPerTick = (int) (plugin.getConfig().getDouble("max_task_millis_per_tick", 2.5D) * 1E6);
    }

    /**
     * Queue of scheduled tasks' operations to run
     */
    private final ConcurrentLinkedQueue<ICoroutine> scheduledTasks = new ConcurrentLinkedQueue<ICoroutine>(); 

    @Override
    public void run() {
        final long stopTime = System.nanoTime() + this.maxNanosPerTick;
        
        // Maximum tasks that can be run in one tick is one entire run through of the queue
        final int maxLength = this.scheduledTasks.size();
        for (int i = 0; i < maxLength; ++i) {
            final ICoroutine coroutine;
            
            // Checks if we have exceeded allowed time
            // And gets the next task to run; if no more tasks, return
            if (System.nanoTime() > stopTime || ((coroutine = this.scheduledTasks.poll()) == null)) return;

            //Run task operation
            final boolean shouldBeRescheduled = coroutine.tick();

            //Schedule again if necessary
            if (shouldBeRescheduled) {
                enqueue(coroutine);
            }
        }
    }

    /**
     * Adds a task to the queue
     * @param task task to add
     */
    public void enqueue(final ICoroutine task) {
        this.scheduledTasks.add(task);
    }
    
}
