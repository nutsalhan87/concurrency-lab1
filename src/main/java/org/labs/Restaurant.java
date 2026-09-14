package org.labs;

public class Restaurant {
    private Groupex groupex;

    public Restaurant(int programmers, int food, int garcons) throws IllegalArgumentException {
        if (programmers <= 0) {
            throw new IllegalArgumentException("There should be more than 0 programmers");
        }
        if (food <= 0) {
            throw new IllegalArgumentException("Do you know that restaurant needs food?");
        }
        if (garcons <= 0) {
            throw new IllegalArgumentException("There should be more than 0 garcons");
        }

        this.groupex = new Groupex(programmers);
    }
}
