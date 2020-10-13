package uk.gov.justice.digital.hmpps.oauth2server.config;

import io.swagger.util.ReferenceSerializationConfigurer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.json.JacksonModuleRegistrar;
import springfox.documentation.spring.web.plugins.Docket;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket offenderApi(final BuildProperties buildProperties) {
        final var docket = new Docket(DocumentationType.OAS_30)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo(buildProperties))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();

        docket.genericModelSubstitutes(Optional.class);
        docket.directModelSubstitute(ZonedDateTime.class, java.util.Date.class);
        docket.directModelSubstitute(LocalDateTime.class, java.util.Date.class);

        return docket;
    }

    private ApiInfo apiInfo(final BuildProperties buildProperties) {
        return new ApiInfo(
                "HMPPS Auth",
                "HMPPS Auth API Documentation",
                buildProperties.getVersion(), "", contactInfo(), "Open Government Licence v3.0", "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
                Collections.emptyList());
    }

    private Contact contactInfo() {
        return new Contact(
                "HMPPS Digital Studio",
                "",
                "feedback@digital.justice.gov.uk");
    }
//
//    @Bean
//    public JacksonModuleRegistrar swaggerJacksonModuleRegistrar() {
//        return ReferenceSerializationConfigurer::serializeAsComputedRef;
//    }
}
