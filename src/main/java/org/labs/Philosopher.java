package org.labs;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

public class Philosopher extends Thread {
    private final Semaphore foodServedNotifier = new Semaphore(0, false);
    private final int[] forkIndexes;
    private final List<? extends Lock> forks;
    private final BlockingQueue<Semaphore> orderBus;
    private final CountDownLatch startDinnerSignal;
    private long eaten = 0;
    
    public Philosopher(int idx, int philosophers, List<? extends Lock> forks, BlockingQueue<Semaphore> orderBus, CountDownLatch startDinnerSignal) {
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
                forks.get(forkIndexes[0]).lock();
                forks.get(forkIndexes[1]).lock();
                eaten++;
                forks.get(forkIndexes[0]).unlock();
                forks.get(forkIndexes[1]).unlock();
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
