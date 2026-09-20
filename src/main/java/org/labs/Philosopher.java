package org.labs;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Philosopher extends Thread {
    private final Semaphore foodServedNotifier = new Semaphore(0, false);
    private final int[] forkIndexes;
    private final Groupex forks;
    private final BlockingQueue<Semaphore> orderBus;
    private final CountDownLatch startDinnerSignal;
    private long eaten = 0;
    
    public Philosopher(int idx, int philosophers, Groupex forks, BlockingQueue<Semaphore> orderBus, CountDownLatch startDinnerSignal) {
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
                orderBus.put(foodServedNotifier);
                foodServedNotifier.acquire();
                forks.lock(forkIndexes[0]);
                forks.lock(forkIndexes[1]);
                eaten++;
                forks.unlock(forkIndexes[0]);
                forks.unlock(forkIndexes[1]);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
