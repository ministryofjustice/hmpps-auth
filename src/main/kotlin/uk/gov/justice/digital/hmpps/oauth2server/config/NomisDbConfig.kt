package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.oauth2server.nomis.repository"])
@ConfigurationProperties(prefix = "spring")
class NomisDbConfig {
  lateinit var jpa: Map<String, String>
  lateinit var datasource: Map<String, String>

  @Primary
  @Bean(name = ["dataSource"])
  fun dataSource(): DataSource = DataSourceBuilder.create()
    .url(datasource["url"])
    .username(datasource["username"])
    .password(datasource["password"])
    .build()

  @Primary
  @Bean(name = ["entityManagerFactory"])
  fun entityManagerFactory(
    builder: EntityManagerFactoryBuilder,
    @Qualifier("dataSource") dataSource: DataSource
  ): LocalContainerEntityManagerFactoryBean = builder
    .dataSource(dataSource)
    .packages("uk.gov.justice.digital.hmpps.oauth2server.nomis.model")
    .persistenceUnit("nomis")
    .properties(jpa)
    .build()

  @Primary
  @Bean(name = ["transactionManager"])
  fun transactionManager(
    @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory
  ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
