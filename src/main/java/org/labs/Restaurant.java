package org.labs;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class Restaurant {
    private final List<Garcon> garcons;
    private final List<Philosopher> philosophers;
    private final CountDownLatch startDinnerSignal = new CountDownLatch(1);
    private final Semaphore outOfFoodSignal = new Semaphore(0);
    private final ExecutorService workingGarcons = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService eatingPhilosophers = Executors.newVirtualThreadPerTaskExecutor();

    public Restaurant(int philosophers, long food, int garcons, boolean multipleBuses) throws IllegalArgumentException {
        if (philosophers <= 1) {
            throw new IllegalArgumentException("There should be more than 1 philosophers");
        }
        if (food <= 0) {
            throw new IllegalArgumentException("Do you know that restaurant needs food?");
        }
        if (garcons <= 0) {
            throw new IllegalArgumentException("There should be more than 0 garcons");
        }

        var foodPool = new AtomicLong(food);

        int orderBusesCount = multipleBuses ? Math.min(garcons, philosophers) : 1;
        var orderBuses = IntStream
            .range(0, orderBusesCount)
            .mapToObj(_ -> new LinkedBlockingQueue<Semaphore>(philosophers / orderBusesCount + 1))
            .toList();
        this.garcons = IntStream
            .range(0, garcons)
            .mapToObj(idx -> new Garcon(foodPool, orderBuses.get(idx % orderBusesCount), outOfFoodSignal))
            .toList();
        this.philosophers = IntStream
            .range(0, philosophers)
            .mapToObj(idx -> new Philosopher(orderBuses.get(idx % orderBusesCount), startDinnerSignal))
            .toList();
        IntStream
            .range(0, philosophers)
            .forEach(idx -> {
                var p = this.philosophers.get(idx);
                p.setNeighbours(
                    this.philosophers.get((idx - 1 + philosophers) % philosophers),
                    this.philosophers.get((idx + 1) % philosophers)
                );
                if (idx % 2 == 0) {
                    if (idx != philosophers - 1) {
                        p.giveRightFork();
                    }
                    p.giveLeftFork();
                }
            });
    }

    public void start() {
        this.garcons.forEach(this.workingGarcons::execute);
        this.philosophers.forEach(this.eatingPhilosophers::execute);
        this.startDinnerSignal.countDown();
    }

    public void join() throws InterruptedException {
        this.outOfFoodSignal.acquire();
        this.workingGarcons.shutdownNow();
        this.eatingPhilosophers.shutdownNow();
    }

    public Stats getStats() {
        var meanServed = this.garcons
            .stream()
            .mapToLong(garcon -> garcon.getServed())
            .average()
            .orElse(0);
        var stdServed = Math.sqrt(this.garcons
            .stream()
            .mapToDouble(garcon -> Math.pow(garcon.getServed() - meanServed, 2))
            .average()
            .orElse(0));

        var meanEaten = this.philosophers
            .stream()
            .mapToLong(garcon -> garcon.getEaten())
            .average()
            .orElse(0);
        var stdEaten = Math.sqrt(this.philosophers
            .stream()
            .mapToDouble(garcon -> Math.pow(garcon.getEaten() - meanEaten, 2))
            .average()
            .orElse(0));

        return new Stats(meanServed, stdServed, meanEaten, stdEaten);
    }

    public static record Stats(double meanServed, double stdServed, double meanEaten, double stdEaten) {}
}
