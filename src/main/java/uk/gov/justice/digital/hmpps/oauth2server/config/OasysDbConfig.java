package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        entityManagerFactoryRef = "oasysEntityManagerFactory",
        transactionManagerRef = "oasysTransactionManager",
        basePackages = {"uk.gov.justice.digital.hmpps.oauth2server.oasys.repository"}
)
@ConfigurationProperties(prefix = "oasys")
@Data
public class OasysDbConfig {

    private Map<String, String> jpa;
    private Map<String, String> datasource;

    @Bean(name = "oasysDataSource")
    public DataSource oasysDataSource() {
        return DataSourceBuilder.create()
                .url(datasource.get("url"))
                .username(datasource.get("username"))
                .password(datasource.get("password"))
                .build();
    }

    @Bean(name = "oasysEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("oasysDataSource") final DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages("uk.gov.justice.digital.hmpps.oauth2server.oasys.model")
                .persistenceUnit("oasys")
                .properties(jpa)
                .build();
    }

    @Bean(name = "oasysTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("oasysEntityManagerFactory") final EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}
