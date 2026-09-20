package org.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RestaurantTest {
    @Test
    void rejectsMissingPhilosophers() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant(0, 1, 1));
    }

    @Test
    void rejectsMissingFood() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant(1, 0, 1));
    }

    @Test
    void rejectsMissingGarcons() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant(1, 1, 0));
    }

    @Test
    void reportsEmptyStatsBeforeStart() {
        var stats = new Restaurant(2, 1, 1).getStats();

        assertEquals(0.0, stats.meanServed());
        assertEquals(0.0, stats.stdServed());
        assertEquals(0.0, stats.meanEaten());
        assertEquals(0.0, stats.stdEaten());
    }
}
