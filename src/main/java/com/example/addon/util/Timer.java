package com.example.addon.util;

public class Timer {
    private long time = -1L;

    public boolean passedMs(long ms) {
        return this.passedNS(this.convertToNS(ms));
    }

    public void setMs(long ms) {
        this.time = System.nanoTime() - this.convertToNS(ms);
    }

    public boolean passedNS(long ns) {
        return System.nanoTime() - this.time >= ns;
    }

    public Timer reset() {
        this.time = System.nanoTime();
        return this;
    }

    public long convertToNS(long time) {
        return time * 1000000L;
    }
}

