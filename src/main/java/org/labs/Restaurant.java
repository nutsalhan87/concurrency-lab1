package org.labs;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Restaurant {
    private final List<Garcon> garcons;
    private final List<Philosopher> philosophers;
    private final CountDownLatch startDinnerSignal = new CountDownLatch(1);

    public Restaurant(int philosophers, long food, int garcons) throws IllegalArgumentException {
        if (philosophers <= 0) {
            throw new IllegalArgumentException("There should be more than 0 philosophers");
        }
        if (food <= 0) {
            throw new IllegalArgumentException("Do you know that restaurant needs food?");
        }
        if (garcons <= 0) {
            throw new IllegalArgumentException("There should be more than 0 garcons");
        }

        var forks = new Groupex(philosophers);
        var orderBus = new LinkedBlockingQueue<Tray>(philosophers);
        var foodPool = new AtomicLong(food);
        this.garcons = Stream
            .generate(() -> new Garcon(foodPool, orderBus))
            .limit(garcons)
            .toList();
        this.philosophers = IntStream
            .range(0, philosophers)
            .mapToObj(idx -> new Philosopher(idx, philosophers, forks, orderBus, startDinnerSignal))
            .toList();
    }

    public void start() {
        garcons.forEach(Thread::start);
        philosophers.forEach(Thread::start);
        this.startDinnerSignal.countDown();
    }

    public void join() throws InterruptedException {
        for (var garcon : garcons) {
            garcon.join();
        }
        for (var philosopher : philosophers) {
            philosopher.interrupt();
        }
    }

    public void printStats() {
        System.out.println("Garcon stats:");
        for (int i = 0; i < garcons.size(); ++i) {
            System.out.println(String.format("Garcon %d: served=%d", i, garcons.get(i).getServed()));
        }
        System.out.println();

        System.out.println("Philosopher stats:");
        for (int i = 0; i < philosophers.size(); ++i) {
            System.out.println(String.format("Philosophers %d: eaten=%d", i, philosophers.get(i).getEaten()));
        }
    }
}
