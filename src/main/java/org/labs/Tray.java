package org.labs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class Tray {
    public final Semaphore notifier = new Semaphore(0, false);
    public final AtomicBoolean food = new AtomicBoolean(false);
}
