package org.labs;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Restaurant restaurant = new Restaurant(700, 1000000, 200);

        long start = System.currentTimeMillis();
        restaurant.start();
        restaurant.join();
        long elapsed = System.currentTimeMillis() - start;

        var stats = restaurant.getStats();
        System.out.printf("Garcons served = %.2f ± %.2f each\n", stats.meanServed(), stats.stdServed());
        System.out.printf("Philosophers eaten = %.2f ± %.2f each\n", stats.meanEaten(), stats.stdEaten());
        System.out.printf("Restaurant served all philosophers in %d ms\n", elapsed);
    }
}