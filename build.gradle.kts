plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "1.0.2"
  id("groovy")
  kotlin("plugin.spring") version "1.4.0"
  kotlin("plugin.jpa") version "1.4.0"
}

repositories {
  maven("https://dl.bintray.com/gov-uk-notify/maven")
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  annotationProcessor("org.projectlombok:lombok:1.18.12")

  implementation(files("lib/ojdbc10-19.3.jar"))

  implementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
  implementation("org.springframework.security.oauth:spring-security-oauth2:2.5.0.RELEASE")
  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("com.nimbusds:nimbus-jose-jwt:8.20")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.3")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("javax.activation:activation:1.1.1")

  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")

  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity5")
  implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:2.4.1")
  implementation("uk.gov.service.notify:notifications-java-client:3.17.0-RELEASE")

  implementation("org.flywaydb:flyway-core:6.5.5")
  implementation("com.zaxxer:HikariCP:3.4.5")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("com.microsoft.sqlserver:mssql-jdbc:8.4.1.jre11")
  implementation("io.swagger:swagger-core:1.6.2")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.springframework.boot:spring-boot-devtools")

  compileOnly("org.projectlombok:lombok:1.18.12")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.12")
  testCompileOnly("org.projectlombok:lombok:1.18.12")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux")

  testImplementation("org.codehaus.groovy:groovy-all:3.0.5")
  testImplementation("org.spockframework:spock-spring:1.3-groovy-2.5")
  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(mapOf("group" to "org.codehaus.groovy"))
  }

  testImplementation("org.gebish:geb-core:3.4")
  testImplementation("org.gebish:geb-spock:3.4")
  testImplementation("org.seleniumhq.selenium:selenium-support:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-firefox-driver:3.141.59")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")

  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.1")
  testImplementation("com.github.tomjankes:wiremock-groovy:0.2.0")
  testImplementation("org.slf4j:slf4j-api:1.7.30")
  testImplementation("com.auth0:java-jwt:3.10.3")

  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.18.1")
  testImplementation("org.fluentlenium:fluentlenium-junit-jupiter:4.5.1")
  testImplementation("org.fluentlenium:fluentlenium-assertj:4.5.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.0.21")
}

tasks {
  test {
    useJUnitPlatform()
    exclude("**/integration/*")
  }

  val testFluentIntegration by registering(Test::class) {
    systemProperty("fluentlenium.capabilities", """{"chromeOptions": {"args": ["headless","disable-gpu","disable-extensions","no-sandbox","disable-application-cache"]}}""")
    useJUnitPlatform()
    include("uk/gov/justice/digital/hmpps/oauth2server/integration/*")
    setMaxHeapSize("256m")
  }

  val testIntegration by registering(Test::class) {
    systemProperty("geb.env", "chromeHeadless")
    include("uk/gov/justice/digital/hmpps/oauth2server/integration/specs/*")
    setMaxHeapSize("256m")
  }
}
