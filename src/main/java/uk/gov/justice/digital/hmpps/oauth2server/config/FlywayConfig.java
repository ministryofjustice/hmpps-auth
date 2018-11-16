package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @FlywayDataSource
    @Primary
    public Flyway primaryFlyway(@Qualifier("authDataSource") DataSource authDataSource,
        @Value("${auth.flyway.locations:db/migration}") List<String> flywayLocations) {
        Flyway flyway = Flyway.configure()
                .dataSource(authDataSource)
                .locations(flywayLocations.toArray(new String[0]))
                .load();
        flyway.migrate();
        return flyway;
    }

}
