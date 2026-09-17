package org.labs;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Garcon extends Thread {
    private final AtomicLong food;
    private final LinkedBlockingQueue<Tray> orderBus;
    private long served = 0;

    public Garcon(AtomicLong food, LinkedBlockingQueue<Tray> orderBus) {
        this.food = food;
        this.orderBus = orderBus;
    }

    public long getServed() {
        return served;
    }

    @Override
    public void run() {
        while (food.get() > 0) {
            Tray tray = null;
            try {
                tray = orderBus.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return;
            }
            if (tray == null) {
                continue;
            }
            while (true) {
                var currentFood = food.get();
                if (currentFood <= 0) {
                    tray.notifier.release();
                    return;
                }
                if (food.compareAndSet(currentFood, currentFood - 1)) {
                    served++;
                    break;
                }
                Thread.onSpinWait();
            }
            tray.food.set(true);
            tray.notifier.release();
        }
    }
    
}
