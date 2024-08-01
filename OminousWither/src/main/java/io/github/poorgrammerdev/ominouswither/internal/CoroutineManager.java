package io.github.poorgrammerdev.ominouswither.internal;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Manages running operations of tasks that have been split across multiple ticks with a CPU time limitation
 * Please note that these tasks are running on the Main thread; they are not async.
 * Referencing code from this thread: https://www.spigotmc.org/threads/guide-on-workload-distribution-or-how-to-handle-heavy-splittable-tasks.409003/
 */
public class CoroutineManager extends BukkitRunnable {
    //Singleton pattern
    private static final CoroutineManager instance = new CoroutineManager();
    public static CoroutineManager getInstance() { return instance; }

    private static double MAX_MILLIS_PER_TICK = 2.5;
    private static final int MAX_NANOS_PER_TICK = (int) (MAX_MILLIS_PER_TICK * 1E6);

    /**
     * Queue of scheduled tasks' operations to run
     */
    private final ConcurrentLinkedQueue<ICoroutine> scheduledTasks = new ConcurrentLinkedQueue<ICoroutine>(); 

    @Override
    public void run() {
        //TODO: FIXME: remove
        Bukkit.getServer().getOnlinePlayers().forEach((p) -> {p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(scheduledTasks.size() + ""));});

        long stopTime = System.nanoTime() + MAX_NANOS_PER_TICK;
        
        // Maximum tasks that can be run in one tick is one entire run through of the queue
        final int maxLength = this.scheduledTasks.size();
        for (int i = 0; i < maxLength; ++i) {
            final ICoroutine coroutine;
            
            // Checks if we have exceeded allowed time
            // And gets the next task to run; if no more tasks, return
            if (System.nanoTime() > stopTime || ((coroutine = this.scheduledTasks.poll()) == null)) return;

            //Run task operation
            coroutine.tick();

            //Schedule again if necessary
            if (coroutine.shouldBeRescheduled()) {
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
