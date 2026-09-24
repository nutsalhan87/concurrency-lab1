package org.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class PhilosopherTest {
    @Test
    void eatsAfterOrderIsServed() throws InterruptedException {
        var orders = new LinkedBlockingQueue<java.util.concurrent.Semaphore>();
        var philosopher = new Philosopher(orders);
        philosopher.giveLeftFork();
        philosopher.giveRightFork();

        philosopher.start();
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
