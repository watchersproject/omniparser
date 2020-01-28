package io.github.thecarisma.util;

import java.io.IOException;

public interface TRunnable extends Runnable {
    boolean isRunning();
    void stop() throws Exception;
}
