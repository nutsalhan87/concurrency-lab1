package org.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class GarconTest {
    @Test
    void servesQueuedOrder() throws InterruptedException {
        var food = new AtomicLong(1);
        var orders = new LinkedBlockingQueue<Semaphore>();
        var served = new Semaphore(0);
        var garcon = new Garcon(food, orders, new Semaphore(0));

        garcon.start();
        orders.put(served);
        try {
            assertTrue(served.tryAcquire(1, TimeUnit.SECONDS));
        } finally {
            garcon.interrupt();
            garcon.join(1_000);
        }

        assertFalse(garcon.isAlive());
        assertEquals(0, food.get());
        assertEquals(1, garcon.getServed());
    }

    @Test
    void exitsImmediatelyWithoutFood() throws InterruptedException {
        var garcon = new Garcon(new AtomicLong(0), new LinkedBlockingQueue<>(), new Semaphore(0));

        garcon.start();
        garcon.join(1_000);

        assertFalse(garcon.isAlive());
        assertEquals(0, garcon.getServed());
    }

    @Test
    void stopsWhenInterruptedWhileWaitingForOrder() throws InterruptedException {
        var food = new AtomicLong(1);
        var garcon = new Garcon(food, new LinkedBlockingQueue<>(), new Semaphore(0));

        garcon.start();
        garcon.interrupt();
        garcon.join(1_000);

        assertFalse(garcon.isAlive());
        assertEquals(1, food.get());
        assertEquals(0, garcon.getServed());
    }
}
