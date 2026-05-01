package de.jugda.registration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class PostgresTestcontainersResource implements QuarkusTestResourceLifecycleManager {



    private static PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14"));
        postgres.start();

        Map<String, String> config = new HashMap<>();

        config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        config.put("quarkus.datasource.username", postgres.getUsername());
        config.put("quarkus.datasource.password", postgres.getPassword());

        config.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        return config;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
