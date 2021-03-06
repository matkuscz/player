

package util.async.executor;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.reactfx.util.Timer;
import util.dev.Dependency;

/**
* Provides factory methods for timers that are manipulated from and execute
* their action on the JavaFX application thread.
* <p>
* @author Tomas Mikula
*/
public class FxTimer implements Timer {
    
    private final Timeline timeline;
    @Dependency(value = "name - field is accessed with reflection")
    private final Runnable action;
    
    private Duration period;
    private long seq = 0;
    
    /**
    * Creates a (stopped) timer that executes the given action specified number
    * of times with a delay period.
    * @param delay Time to wait before each execution. The first execution is 
    * already delayed.
    * @param action action to execute
    * @param cycles denotes number of executions. Use 1 for single execution, n
    * for n executions and {@link Animation.INDEFINITE} (-1) for infinite amount.
    */
    public FxTimer(Duration delay, int cycles, Runnable action) {
        this.period = Duration.millis(delay.toMillis());
        this.timeline = new Timeline();
        this.action = action;

        timeline.setCycleCount(cycles);
    }
    /**
    * Equivalent to {@code new FxTimer(Duration.millis(delay), action, cycles);}
    */
    public FxTimer(double delay, int cycles, Runnable action) {
        this(Duration.millis(delay), cycles, action);
    }

    @Override
    public void restart() {
        restart(period);
    }
    
    /**
     * Equivalent to calling {@link #setTimeout()} and {@link #restart()}
     * subsequently.
     * @param period 
     */
    public void restart(Duration period) {
        stop();
        long expected = seq;
        this.period = period;
        timeline.getKeyFrames().setAll(new KeyFrame(period, ae -> {
            if(seq == expected) {
                action.run();
            }
        }));
        timeline.play();
    }

    public void restart(double periodInMs) {
        restart(Duration.millis(periodInMs));
    }
    
    public void pause() {
        timeline.pause();
    }
    
    public void unpause() {
        timeline.play();
    }
    
    @Override
    public void stop() {
        timeline.stop();
        seq++;
    }
    
    public boolean isRunning() {
        return timeline.getCurrentRate()!=0;
    }
    
    /**
     * Sets the delay for the task. Takes effect only if set before the task
     * execution is planned. It will not affect currently running cycle. It will
     * affect every subsequent cycle. Therefore, it is pointless to run this
     * method if this timer is non-periodic.
     * @param period 
     */
    public void setPeriod(Duration period) {
        this.period = period;
    }
    
    public void setPeriod(double periodInMs) {
        this.period = Duration.millis(periodInMs);
    }
    
    public Duration getPeriod() {
        return period;
    }
    
    /**
     * If timer running, executes {@link #restart(javafx.util.Duration)}, else
     * executes {@link #setPeriod(javafx.util.Duration)}
     * <p>
     * Basically same as {@link #restart(javafx.util.Duration)}, but restarts
     * only if needed (when running).
     * 
     * @param timeout 
     */
    public void setTimeoutAndRestart(Duration timeout) {
        if (isRunning()) restart(timeout);
        else FxTimer.this.setPeriod(timeout);
    }
    
    public void setTimeoutAndRestart(double timeautInMillis) {
        if (isRunning()) restart(Duration.millis(timeautInMillis));
        else FxTimer.this.setPeriod(Duration.millis(timeautInMillis));
    }
}