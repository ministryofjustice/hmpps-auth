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
        entityManagerFactoryRef = "authEntityManagerFactory",
        transactionManagerRef = "authTransactionManager",
        basePackages = {"uk.gov.justice.digital.hmpps.oauth2server.auth.repository"}
)
@ConfigurationProperties(prefix = "auth")
@Data
public class AuthDbConfig {

    private Map<String, String> jpa;
    private Map<String, String> datasource;

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        return DataSourceBuilder.create()
                .url(datasource.get("url"))
                .username(datasource.get("username"))
                .password(datasource.get("password"))
                .build();
    }

    @Bean(name = "authEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("authDataSource") DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages("uk.gov.justice.digital.hmpps.oauth2server.auth.model")
                .persistenceUnit("auth")
                .properties(jpa)
                .build();
    }

    @Bean(name = "authTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("authEntityManagerFactory") EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}
