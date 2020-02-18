package me.shawlaf.varlight.fabric;

public interface IScheduledTaskManager extends IModComponent {

    void enqueue(Runnable task);

    Runnable dequeue();

    boolean isEmpty();

    default void runNext() {
        dequeue().run();
    }

}
