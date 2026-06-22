package com.tinywas.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerConfig {
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_BACKLOG = 50;
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    private static final String DEFAULT_STATIC_ROOT = "static";

    private final int port;
    private final int backlog;
    private final Path staticRoot;

    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.backlog = builder.backlog;
        this.staticRoot = builder.staticRoot;
    }

    public int getPort() { return this.port; }
    public int getBacklog() { return this.backlog; }
    public Path getStaticRoot() { return this.staticRoot; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int port = DEFAULT_PORT;
        private int backlog = DEFAULT_BACKLOG;
        private Path staticRoot = Paths.get(DEFAULT_STATIC_ROOT);

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder backlog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        public Builder staticRoot(String path) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("staticRoot must not be null or blank");
            }

            this.staticRoot = Paths.get(path);
            return this;
        }

        public ServerConfig build() {
            validatePort(port);
            validateBacklog(backlog);
            this.staticRoot = validateAndNormalize(staticRoot);
            return new ServerConfig(this);
        }

        private void validatePort(int port) {
            if (port < MIN_PORT || port > MAX_PORT) {
                throw new IllegalArgumentException(
                        "port must be between " + MIN_PORT + " and " + MAX_PORT + ": " + port);
            }
        }

        private void validateBacklog(int backlog) {
            if (backlog <= 0) {
                throw new IllegalArgumentException("backlog must be positive: " + backlog);
            }
        }

        private Path validateAndNormalize(Path path) {
            Path absolute = path.toAbsolutePath().normalize();
            if (!Files.exists(absolute)) {
                throw new IllegalArgumentException("staticRoot does not exist: " + absolute);
            }
            if (!Files.isDirectory(absolute)) {
                throw new IllegalArgumentException("staticRoot is not a directory: " + absolute);
            }
            return absolute;
        }
    }
}
