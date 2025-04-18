package dobby.task;

import dobby.Config;
import common.logger.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {
    private static SchedulerService instance;
    private final ArrayList<ScheduledExecutorService> schedulers = new ArrayList<>();
    private static final Logger LOGGER = new Logger(SchedulerService.class);

    public static SchedulerService getInstance() {
        if (instance == null) {
            instance = new SchedulerService();
        }
        return instance;
    }

    private SchedulerService() {
    }

    public void addRepeating(Runnable task, int interval, TimeUnit unit) {
        if (isDisabled()) {
            LOGGER.warn("Scheduler is disabled, not scheduling task");
            return;
        }
        final ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task, 0, interval, unit);
        schedulers.add(scheduler);
    }

    public void stopAll() {
        schedulers.forEach(ExecutorService::shutdown);
    }

    private boolean isDisabled() {
        return Config.getInstance().getBoolean("dobby.scheduler.disabled", false);
    }
}
