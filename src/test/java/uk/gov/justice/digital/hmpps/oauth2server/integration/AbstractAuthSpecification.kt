package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.adapter.junit.jupiter.FluentTest
import org.fluentlenium.core.FluentPage
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.FindBy
import uk.gov.justice.digital.hmpps.oauth2server.resource.AzureOIDCExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.TokenVerificationExtension

@ExtendWith(DeliusExtension::class, TokenVerificationExtension::class, AzureOIDCExtension::class)
open class AbstractAuthSpecification : FluentTest() {
  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @Page
  internal lateinit var loginPage: LoginPage

  @AfterEach
  fun cleanup() {
    try {
      executeScript("console.log('Test finished')")
      val logEntries = driver.manage().logs().get(LogType.BROWSER).all
      println("START WebDriver ${LogType.BROWSER} logs")
      logEntries.forEach { println(it) }
      println("END WebDriver ${LogType.BROWSER} logs")
    } catch (error: Exception) {
      error.printStackTrace()
    }
  }
}

@Suppress("UNCHECKED_CAST")
open class AuthPage<T>(
  private val title: String,
  private val heading: String,
  private val headingStartsWith: Boolean = false,
) : FluentPage() {
  @FindBy(css = "#main-content h1")
  private lateinit var headingText: FluentWebElement

  @FindBy(css = "#error-detail")
  private lateinit var errorDetail: FluentWebElement

  @FindBy(css = "#logout")
  private lateinit var logOut: FluentWebElement

  @FindBy(css = "#principal-name")
  private lateinit var principalName: FluentWebElement

  internal fun logOut() {
    logOut.click()
  }

  override fun isAt() {
    super.isAt()

    assertThat(window().title()).isEqualTo(title)
    if (headingStartsWith) {
      assertThat(headingText.text()).startsWith(heading)
    } else {
      assertThat(headingText.text()).isEqualTo(heading)
    }
  }

  internal fun isAtPage(): T {
    isAt()
    return this as T
  }

  private fun isAtError() {
    super.isAt()

    assertThat(window().title()).isEqualTo("Error: $title")
    assertThat(headingText.text()).isEqualTo(heading)
  }

  internal fun checkError(error: String): T {
    isAtError()
    assertThat(errorDetail.text()).isEqualTo(error)
    return this as T
  }

  internal fun checkErrorContains(error: String): T {
    isAtError()
    assertThat(errorDetail.text()).contains(error)
    return this as T
  }

  internal fun parseJwt(): JWTClaimsSet {
    val token = driver.manage().getCookieNamed("jwtSession").value
    return JWTParser.parse(token).jwtClaimsSet
  }

  internal fun assertNameDisplayedCorrectly(name: String) {
    assertThat(principalName.text()).isEqualTo(name)
  }

  internal fun getCurrentName(): String = principalName.text()
}
