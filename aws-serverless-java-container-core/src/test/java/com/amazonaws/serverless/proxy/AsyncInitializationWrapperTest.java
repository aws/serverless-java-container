package com.amazonaws.serverless.proxy;

import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsyncInitializationWrapperTest {

    @Test
    void initCreate_noStartTime_setsCurrentTime() {
        AsyncInitializationWrapper init = new AsyncInitializationWrapper();
        long initTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        assertEquals(initTime, init.getActualStartTimeMs());
    }

    @Test
    void initCreate_withStartTime_storesCustomStartTime() throws InterruptedException {
        long initTime = Instant.now().toEpochMilli();
        Thread.sleep(500);
        AsyncInitializationWrapper init = new AsyncInitializationWrapper(initTime);

        assertEquals(initTime, init.getActualStartTimeMs());
    }
}
