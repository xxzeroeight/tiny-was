package com.tinywas.server;

public class KeepAliveConfig {
    private final int maxRequests;
    private final int timeoutMillis;

    private KeepAliveConfig(Builder builder) {
        this.maxRequests = builder.maxRequests;
        this.timeoutMillis = builder.timeoutMillis;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRequests = 100;
        private int timeoutMillis = 5000;

        public Builder maxRequests(int maxRequests) {
            if (maxRequests < 1) {
                throw new IllegalArgumentException("maxRequests must be at least 1");
            }

            this.maxRequests = maxRequests;
            return this;
        }

        public Builder timeoutMillis(int timeoutMillis) {
            if (timeoutMillis < 0) {
                throw new IllegalArgumentException("timeoutMillis must be non-negative");
            }

            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public KeepAliveConfig build() {
            return new KeepAliveConfig(this);
        }
    }
}
