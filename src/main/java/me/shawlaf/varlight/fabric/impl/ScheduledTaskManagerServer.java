package me.shawlaf.varlight.fabric.impl;

import me.shawlaf.varlight.fabric.IScheduledTaskManager;

import java.util.LinkedList;

public class ScheduledTaskManagerServer implements IScheduledTaskManager {

    private final LinkedList<Runnable> queue = new LinkedList<>();

    @Override
    public void enqueue(Runnable task) {
        synchronized (queue) {
            queue.add(task);
        }
    }

    @Override
    public Runnable dequeue() {
        synchronized (queue) {
            return queue.removeFirst();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

    @Override
    public void onServerShutdown() {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.removeFirst().run();
            }
        }
    }
}
