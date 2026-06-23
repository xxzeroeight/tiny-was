package com.tinywas.server;

public class ThreadPoolConfig {
    private final int corePoolSize;
    private final long shutdownTimeoutSeconds;

    private ThreadPoolConfig(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.shutdownTimeoutSeconds = builder.shutdownTimeoutSeconds;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public long getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int corePoolSize = 50;
        private long shutdownTimeoutSeconds = 30;

        public Builder corePoolSize(int corePoolSize) {
            if (corePoolSize < 1) {
                throw new IllegalArgumentException("corePoolSize must be at least 1");
            }
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder shutdownTimeoutSeconds(long seconds) {
            if (seconds < 0) {
                throw new IllegalArgumentException("shutdownTimeoutSeconds must be non-negative");
            }
            this.shutdownTimeoutSeconds = seconds;
            return this;
        }

        public ThreadPoolConfig build() {
            return new ThreadPoolConfig(this);
        }
    }
}
