package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FlywayConfig {
    private static final String VENDOR_PLACEHOLDER = "{vendor}";

    @Bean(name = "authFlyway", initMethod = "migrate")
    @FlywayDataSource
    public Flyway authFlyway(@Qualifier("authDataSource") final DataSource authDataSource,
                             @Value("${auth.flyway.locations}") final List<String> flywayLocations) {
        final var locations = flywayLocations.toArray(new String[0]);

        // copied from FlywayAutoConfiguration to replace {vendor} so can have different config for h2
        final var replacedLocations = replaceVendorLocations(locations, getDatabaseDriver(authDataSource));
        final var flyway = Flyway.configure()
                .dataSource(authDataSource)
                .locations(replacedLocations)
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean(name = "nomisFlyway", initMethod = "migrate")
    @FlywayDataSource
    @Primary
    @Profile("nomis-seed")
    public Flyway nomisFlyway(@Qualifier("dataSource") final DataSource dataSource,
                              @Value("${nomis.flyway.locations}") final List<String> flywayLocations) {
        final var flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations.toArray(new String[0]))
                .installedBy("nomis-seed")
                .load();
        flyway.migrate();
        return flyway;
    }

    private String[] replaceVendorLocations(final String[] locations, final DatabaseDriver databaseDriver) {
        if (databaseDriver == DatabaseDriver.UNKNOWN) {
            return locations;
        }
        final var vendor = databaseDriver.getId();
        return Arrays.stream(locations).map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor))
                .toArray(String[]::new);
    }

    private DatabaseDriver getDatabaseDriver(final DataSource dataSource) {
        try {
            final String url = JdbcUtils.extractDatabaseMetaData(dataSource, "getURL");
            return DatabaseDriver.fromJdbcUrl(url);
        } catch (final MetaDataAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
