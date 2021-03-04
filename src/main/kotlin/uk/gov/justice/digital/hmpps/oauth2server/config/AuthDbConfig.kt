package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  entityManagerFactoryRef = "authEntityManagerFactory",
  transactionManagerRef = "authTransactionManager",
  basePackages = ["uk.gov.justice.digital.hmpps.oauth2server.auth.repository"]
)
@ConfigurationProperties(prefix = "auth")
class AuthDbConfig {
  lateinit var jpa: Map<String, String>
  lateinit var datasource: Map<String, String>

  @Bean(name = ["authDataSource"])
  @SpringSessionDataSource
  fun authDataSource(): DataSource = DataSourceBuilder.create()
    .url(datasource["url"])
    .username(datasource["username"])
    .password(datasource["password"])
    .build()

  @Bean(name = ["authEntityManagerFactory"])
  fun entityManagerFactory(
    builder: EntityManagerFactoryBuilder,
    @Qualifier("authDataSource") dataSource: DataSource,
  ): LocalContainerEntityManagerFactoryBean = builder
    .dataSource(dataSource)
    .packages("uk.gov.justice.digital.hmpps.oauth2server.auth.model")
    .persistenceUnit("auth")
    .properties(jpa)
    .build()

  @Bean(name = ["authTransactionManager"])
  fun transactionManager(
    @Qualifier("authEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
  ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
