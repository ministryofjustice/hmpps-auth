package uk.gov.justice.digital.hmpps.oauth2server.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.boot.jdbc.DatabaseDriver.UNKNOWN
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.jdbc.support.MetaDataAccessException
import javax.sql.DataSource

@Configuration
class FlywayConfig {
  @Bean(name = ["authFlyway"], initMethod = "migrate")
  @FlywayDataSource
  fun authFlyway(
    @Qualifier("authDataSource") authDataSource: DataSource,
    @Value("\${auth.flyway.locations}") flywayLocations: List<String>
  ): Flyway {
    val flyway = Flyway.configure()
      .dataSource(authDataSource)
      .locations(*replaceVendorLocations(flywayLocations, authDataSource).toTypedArray())
      .load()
    flyway.migrate()
    return flyway
  }

  private fun replaceVendorLocations(locations: List<String>, dataSource: DataSource): List<String> {
    val databaseDriver = getDatabaseDriver(dataSource)
    return replaceVendorLocations(locations, databaseDriver)
  }

  private fun getDatabaseDriver(dataSource: DataSource) =
    try {
      val url = JdbcUtils.extractDatabaseMetaData<String>(dataSource, "getURL")
      DatabaseDriver.fromJdbcUrl(url)
    } catch (ex: MetaDataAccessException) {
      throw IllegalStateException(ex)
    }

  private fun replaceVendorLocations(locations: List<String>, databaseDriver: DatabaseDriver): List<String> {
    if (databaseDriver === UNKNOWN) {
      return locations
    }
    val vendor = databaseDriver.id
    return locations.map { location: String -> location.replace("{vendor}", vendor) }
  }

  @Bean(name = ["nomisFlyway"], initMethod = "migrate")
  @FlywayDataSource
  @Primary
  @Profile("nomis-seed")
  fun nomisFlyway(
    @Qualifier("dataSource") dataSource: DataSource,
    @Value("\${nomis.flyway.locations}") flywayLocations: List<String>
  ): Flyway {
    val flyway = Flyway.configure()
      .dataSource(dataSource)
      .locations(*flywayLocations.toTypedArray())
      .installedBy("nomis-seed")
      .load()
    flyway.migrate()
    return flyway
  }
}
