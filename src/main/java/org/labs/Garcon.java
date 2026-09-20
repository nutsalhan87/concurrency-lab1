package org.labs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class Garcon extends Thread {
    private final AtomicLong food;
    private final BlockingQueue<Semaphore> orderBus;
    private final Semaphore outOfFoodSignal;
    private long served = 0;

    public Garcon(AtomicLong food, BlockingQueue<Semaphore> orderBus, Semaphore outOfFoodSignal) {
        this.food = food;
        this.orderBus = orderBus;
        this.outOfFoodSignal = outOfFoodSignal;
    }

    public long getServed() {
        return served;
    }

    @Override
    public void run() {
        while (food.get() > 0) {
            Semaphore foodServedNotifier;
            try {
                foodServedNotifier = orderBus.take();
            } catch (InterruptedException e) {
                return;
            }
            while (true) {
                var currentFood = this.food.get();
                if (currentFood <= 0) {
                    this.outOfFoodSignal.release();
                    return;
                }
                if (this.food.compareAndSet(currentFood, currentFood - 1)) {
                    this.served++;
                    break;
                }
                Thread.onSpinWait();
            }
            foodServedNotifier.release();
        }
    }
    
}
