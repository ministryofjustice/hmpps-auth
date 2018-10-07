package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    private final DataSource authDataSource;

    @Autowired
    public FlywayConfig(@Qualifier("authDataSource") DataSource authDataSource) {
        this.authDataSource = authDataSource;
    }

    @PostConstruct
    public void migrateFlyway() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(authDataSource);
        flyway.setLocations("db/auth/migration");
        flyway.migrate();
    }
}
