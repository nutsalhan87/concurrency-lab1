package org.labs;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class Philosopher extends Thread {
    private final Semaphore foodServedNotifier = new Semaphore(0, false);
    private final BlockingQueue<Semaphore> orderBus;
    private final Semaphore leftFork = new Semaphore(0);
    private final Semaphore rightFork = new Semaphore(0);
    private Optional<Philosopher> leftPhilosopher = Optional.empty();
    private Optional<Philosopher> rightPhilosopher = Optional.empty();
    private long eaten = 0;
    
    public Philosopher(BlockingQueue<Semaphore> orderBus) {
        this.orderBus = orderBus;
    }

    public void setNeighbours(Philosopher leftPhilosopher, Philosopher rightPhilosopher) {
        this.leftPhilosopher = Optional.ofNullable(leftPhilosopher);
        this.rightPhilosopher = Optional.ofNullable(rightPhilosopher);
    }

    public long getEaten() {
        return eaten;
    }

    public void giveLeftFork() {
        this.leftFork.release();
    }

    public void giveRightFork() {
        this.rightFork.release();
    }

    @Override
    public void run() {
        try {
            while (true) {
                orderBus.put(foodServedNotifier);
                foodServedNotifier.acquire();
                leftFork.acquire();
                rightFork.acquire();
                eaten++;
                leftPhilosopher.ifPresent(Philosopher::giveRightFork);
                rightPhilosopher.ifPresent(Philosopher::giveLeftFork);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
