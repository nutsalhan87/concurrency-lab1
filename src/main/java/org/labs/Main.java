package org.labs;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "restaurant", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    @Option(names = {"-p"})
    private int philosophers = 700;

    @Option(names = {"-g"})
    private int garcons = 200;

    @Option(names = {"-f"})
    private long food = 1000000;

    @Option(names = {"-m"})
    private boolean multipleBuses;

    @Override
    public Integer call() throws Exception {
        Restaurant restaurant = new Restaurant(philosophers, food, garcons, multipleBuses);

        long start = System.currentTimeMillis();
        restaurant.start();
        restaurant.join();
        long elapsed = System.currentTimeMillis() - start;

        var stats = restaurant.getStats();
        System.out.printf("Garcons served = %.2f ± %.2f each\n", stats.meanServed(), stats.stdServed());
        System.out.printf("Philosophers eaten = %.2f ± %.2f each\n", stats.meanEaten(), stats.stdEaten());
        System.out.printf("Restaurant served all philosophers in %d ms\n", elapsed);

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}