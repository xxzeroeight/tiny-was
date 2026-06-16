package com.tinywas.server;

public class ServerConfig {
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_BACKLOG = 50;

    private final int port;
    private final int backlog;

    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.backlog = builder.backlog;
    }

    public int getPort() { return this.port; }
    public int getBacklog() { return this.backlog; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int port = DEFAULT_PORT;
        private int backlog = DEFAULT_BACKLOG;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder backlog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }
}
