package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @FlywayDataSource
    @Primary
    public Flyway primaryFlyway(@Qualifier("authDataSource") DataSource authDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(authDataSource)
                .load();
        flyway.migrate();
        return flyway;
    }

}
