package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {"uk.gov.justice.digital.hmpps.oauth2server.nomis.repository"}
)
@ConfigurationProperties(prefix = "spring")
@Data
public class NomisDbConfig {

    private Map<String, String> jpa;
    private Map<String, String> datasource;

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(datasource.get("url"))
                .username(datasource.get("username"))
                .password(datasource.get("password"))
                .build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") final DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages("uk.gov.justice.digital.hmpps.oauth2server.nomis.model")
                .persistenceUnit("nomis")
                .properties(jpa)
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") final EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
