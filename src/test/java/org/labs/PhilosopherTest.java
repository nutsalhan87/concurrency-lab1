package org.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class PhilosopherTest {
    @Test
    void waitsForDinnerToStart() throws InterruptedException {
        var orders = new LinkedBlockingQueue<java.util.concurrent.Semaphore>();
        var startDinner = new CountDownLatch(1);
        var philosopher = new Philosopher(orders, startDinner);
        philosopher.giveLeftFork();
        philosopher.giveRightFork();

        philosopher.start();
        try {
            assertNull(orders.poll(100, TimeUnit.MILLISECONDS));
        } finally {
            philosopher.interrupt();
            philosopher.join(1_000);
        }

        assertFalse(philosopher.isAlive());
        assertEquals(0, philosopher.getEaten());
    }

    @Test
    void eatsAfterOrderIsServed() throws InterruptedException {
        var orders = new LinkedBlockingQueue<java.util.concurrent.Semaphore>();
        var startDinner = new CountDownLatch(1);
        var philosopher = new Philosopher(orders, startDinner);
        philosopher.giveLeftFork();
        philosopher.giveRightFork();

        philosopher.start();
        startDinner.countDown();
        var served = orders.poll(1, TimeUnit.SECONDS);
        try {
            assertNotNull(served);
            served.release();
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                while (philosopher.getEaten() == 0) {
                    Thread.sleep(10);
                }
            });
        } finally {
            philosopher.interrupt();
            philosopher.join(1_000);
        }

        assertFalse(philosopher.isAlive());
        assertEquals(1, philosopher.getEaten());
    }
}
