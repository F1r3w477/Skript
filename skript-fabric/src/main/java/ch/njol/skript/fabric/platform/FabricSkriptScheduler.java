package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Scheduler that runs tasks on the server thread: immediately, delayed, or repeating.
 */
public final class FabricSkriptScheduler implements SkriptScheduler {

    private final Queue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<DelayedTask> delayedTasks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<RepeatingTask> repeatingTasks = new CopyOnWriteArrayList<>();

    public FabricSkriptScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        long tick = server.getTickCount();

        Runnable task;
        while ((task = mainThreadTasks.poll()) != null) {
            runSafe(task);
        }

        delayedTasks.removeIf(d -> {
            if (d.runAtTick < 0) {
                d.runAtTick = tick + d.delayTicks;
            }
            if (tick >= d.runAtTick) {
                runSafe(d.task);
                return true;
            }
            return false;
        });

        for (RepeatingTask r : repeatingTasks) {
            if (r.nextRunTick <= 0) {
                r.nextRunTick = tick + r.periodTicks;
            }
            if (tick >= r.nextRunTick) {
                runSafe(r.task);
                r.nextRunTick = tick + r.periodTicks;
            }
        }
    }

    private static void runSafe(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void executeSync(Runnable task) {
        if (task != null) {
            mainThreadTasks.add(task);
        }
    }

    @Override
    public void scheduleSyncDelayed(Runnable task, long delayTicks) {
        if (task == null || delayTicks < 0) return;
        delayedTasks.add(new DelayedTask(task, delayTicks));
    }

    @Override
    public void scheduleSyncRepeating(Runnable task, long periodTicks) {
        if (task == null || periodTicks <= 0) return;
        repeatingTasks.add(new RepeatingTask(task, periodTicks));
    }

    private static final class DelayedTask {
        final Runnable task;
        final long delayTicks;
        long runAtTick = -1; // set on first tick to currentTick + delayTicks

        DelayedTask(Runnable task, long delayTicks) {
            this.task = task;
            this.delayTicks = delayTicks;
        }
    }

    private static final class RepeatingTask {
        final Runnable task;
        final long periodTicks;
        long nextRunTick = 0; // set on first tick

        RepeatingTask(Runnable task, long periodTicks) {
            this.task = task;
            this.periodTicks = periodTicks;
        }
    }
}

