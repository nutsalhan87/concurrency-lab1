package org.labs;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Philosopher extends Thread {
    private final Tray tray = new Tray();
    private final int[] forkIndexes;
    private final Groupex forks;
    private final LinkedBlockingQueue<Tray> orderBus;
    private final CountDownLatch startDinnerSignal;
    private long eaten = 0;
    
    public Philosopher(int idx, int philosophers, Groupex forks, LinkedBlockingQueue<Tray> orderBus, CountDownLatch startDinnerSignal) {
        this.forkIndexes = new int[]{ idx, (idx + 1) % philosophers };
        Arrays.sort(this.forkIndexes);
        this.forks = forks;
        this.orderBus = orderBus;
        this.startDinnerSignal = startDinnerSignal;
    }

    public long getEaten() {
        return eaten;
    }

    @Override
    public void run() {
        try {
            startDinnerSignal.await();
            while (true) {
                orderBus.put(this.tray);
                this.tray.notifier.acquire();
                if (!this.tray.food.get()) {
                    return; // no food left
                }
                this.forks.lock(this.forkIndexes[0]);
                this.forks.lock(this.forkIndexes[1]);
                this.tray.food.set(false); // eat
                this.eaten++;
                this.forks.unlock(this.forkIndexes[0]);
                this.forks.unlock(this.forkIndexes[1]);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
