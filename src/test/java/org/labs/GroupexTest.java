package org.labs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroupexTest {
    @Test
    void rejectsNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> new Groupex(0));
        assertThrows(IllegalArgumentException.class, () -> new Groupex(-1));
    }

    @Test
    void locksAndUnlocksForks() {
        var forks = new Groupex(2);

        assertTrue(forks.tryLock(0));
        assertFalse(forks.tryLock(0));
        forks.unlock(0);
        assertTrue(forks.tryLock(0));
        forks.unlock(0);
    }

    @Test
    void locksDifferentForksIndependently() {
        var forks = new Groupex(2);

        assertTrue(forks.tryLock(0));
        assertTrue(forks.tryLock(1));
        forks.unlock(0);
        forks.unlock(1);
    }

    @Test
    void lockMarksForkAsHeld() {
        var forks = new Groupex(2);

        forks.lock(1);
        assertFalse(forks.tryLock(1));
        forks.unlock(1);
    }

    @Test
    void rejectsInvalidIndexes() {
        var forks = new Groupex(1);

        assertThrows(IndexOutOfBoundsException.class, () -> forks.tryLock(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> forks.tryLock(32));
    }
}
