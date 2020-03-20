import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

plugins {
  id("java")
  id("groovy")
  kotlin("jvm") version "1.3.70"
  kotlin("plugin.spring") version "1.3.70"
  kotlin("plugin.jpa") version "1.3.70"
  id("org.springframework.boot") version "2.2.5.RELEASE"
  id("io.spring.dependency-management") version "1.0.9.RELEASE"
  id("org.owasp.dependencycheck") version "5.3.1"
  id("com.github.ben-manes.versions") version "0.28.0"
  id("se.patrikerdes.use-latest-versions") version "0.2.13"
  id("com.gorylenko.gradle-git-properties") version "2.2.2"
}

repositories {
  mavenLocal()
  mavenCentral()
  maven("https://dl.bintray.com/gov-uk-notify/maven")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "11"
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencyCheck {
  failBuildOnCVSS = 5f
  suppressionFiles = listOf("dependency-check-suppress-spring.xml")
  format = ALL
  analyzers.assemblyEnabled = false
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
}

group = "uk.gov.justice.digital.hmpps"

val todaysDate: String = LocalDate.now().format(ISO_DATE)
val today: Instant = Instant.now()
version = if (System.getenv().contains("CI")) "${todaysDate}.${System.getenv("CIRCLE_BUILD_NUM")}" else todaysDate

springBoot {
  buildInfo {
    properties {
      time = today
      additional = mapOf(
          "by" to System.getProperty("user.name"),
          "operatingSystem" to "${System.getProperty("os.name")} (${System.getProperty("os.version")})",
          "continuousIntegration" to System.getenv().containsKey("CI"),
          "machine" to InetAddress.getLocalHost().hostName
      )
    }
  }
}

dependencyManagement {
  imports { mavenBom(SpringBootPlugin.BOM_COORDINATES) }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  annotationProcessor("org.projectlombok:lombok:1.18.12")

  implementation(files("lib/ojdbc10-19.3.jar"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.security:spring-security-jwt:1.1.0.RELEASE")
  implementation("org.springframework.security.oauth:spring-security-oauth2:2.4.0.RELEASE")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("io.jsonwebtoken:jjwt:0.9.1")
  implementation("com.nimbusds:nimbus-jose-jwt:8.10")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.hibernate:hibernate-core")

  implementation("net.logstash.logback:logstash-logback-encoder:6.3")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.0-BETA.3")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.0-BETA.3")
  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("javax.activation:activation:1.1.1")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")

  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity5")
  implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:2.4.1")
  implementation("uk.gov.service.notify:notifications-java-client:3.15.1-RELEASE")

  implementation("org.flywaydb:flyway-core:6.3.1")
  implementation("com.zaxxer:HikariCP:3.4.2")
  implementation("org.apache.commons:commons-lang3:3.9")
  implementation("org.apache.commons:commons-text:1.8")
  implementation("com.microsoft.sqlserver:mssql-jdbc:8.2.1.jre11")
  implementation("com.github.timpeeters:spring-boot-graceful-shutdown:2.2.1")
  implementation("com.google.guava:guava:28.2-jre")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.springframework.boot:spring-boot-devtools")

  compileOnly("org.projectlombok:lombok:1.18.12")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.12")
  testCompileOnly("org.projectlombok:lombok:1.18.12")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux")

  testImplementation("org.codehaus.groovy:groovy-all:3.0.2")
  testImplementation("org.spockframework:spock-spring:1.3-groovy-2.5")
  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(mapOf("group" to "org.codehaus.groovy"))
  }

  testImplementation("org.gebish:geb-core:3.3")
  testImplementation("org.gebish:geb-spock:3.3")
  testImplementation("org.seleniumhq.selenium:selenium-support:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")
  testImplementation("org.seleniumhq.selenium:selenium-firefox-driver:3.141.59")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")

  testImplementation("com.github.tomakehurst:wiremock-standalone:2.26.3")
  testImplementation("com.github.tomjankes:wiremock-groovy:0.2.0")
  testImplementation("org.slf4j:slf4j-api:1.7.30")
  testImplementation("com.auth0:java-jwt:3.10.1")

  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.14.0")
  testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
  testImplementation("org.fluentlenium:fluentlenium-junit-jupiter:4.3.1")
  testImplementation("org.fluentlenium:fluentlenium-assertj:4.3.1")
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

  val agentDeps by configurations.register("agentDeps") {
    dependencies {
      "agentDeps"("com.microsoft.azure:applicationinsights-agent:2.6.0-BETA.3") {
        isTransitive = false
      }
    }
  }

  val copyAgent by registering(Copy::class) {
    from(agentDeps)
    into("$buildDir/libs")
  }

  assemble { dependsOn(copyAgent) }

  bootJar {
    manifest {
      attributes("Implementation-Version" to rootProject.version, "Implementation-Title" to rootProject.name)
    }
  }
}
