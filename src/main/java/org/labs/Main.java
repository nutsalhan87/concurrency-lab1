package org.labs;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Restaurant restaurant = new Restaurant(7, 1000000, 2);
        restaurant.start();
        restaurant.join();
        restaurant.printStats();
    }
}