/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal.testutils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Timer {
    private volatile static Map<String, TimerInfo> timers = new LinkedHashMap<>();
    private volatile static boolean enabled = false;

    public static void start(String timerName) {
        if (!enabled) {
            return;
        }

        timers.put(timerName, new TimerInfo(System.currentTimeMillis()));
    }

    public static long stop(String timerName) {
        if (!enabled) {
            return 0L;
        }

        TimerInfo info = timers.get(timerName);
        if (info == null) {
            throw new IllegalArgumentException("Could not find timer " + timerName);
        }

        long stopTime = System.currentTimeMillis();
        info.stop(stopTime);

        return stopTime;
    }


    public static Map<String, TimerInfo> getTimers() {
        return timers;
    }

    public static TimerInfo getTimer(String timerName) {
        return timers.get(timerName);
    }

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    private static class TimerInfo {
        private long startTime;
        private long stopTime;
        private long duration;

        public TimerInfo(long start) {
            startTime = start;
        }

        public void stop(long stop) {
            stopTime = stop;
            duration = stopTime - startTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getStopTime() {
            return stopTime;
        }


        public long getDuration() {
            return duration;
        }
    }
}
