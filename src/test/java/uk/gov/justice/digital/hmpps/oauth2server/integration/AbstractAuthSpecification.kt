package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.github.tomakehurst.wiremock.WireMockServer
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.adapter.junit.jupiter.FluentTest
import org.fluentlenium.core.FluentPage
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.FindBy

open class AbstractAuthSpecification : FluentTest() {
  @Page
  internal lateinit var loginPage: LoginPage

  @BeforeEach
  fun resetStubs() {
    communityApi.resetAll()
  }

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

  companion object {
    @JvmField
    internal val communityApi = CommunityApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      communityApi.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      communityApi.stop()
    }
  }
}

open class AuthPage(val title: String, val heading: String) : FluentPage() {
  @FindBy(css = "#main-content h1")
  private lateinit var headingText: FluentWebElement

  override fun isAt() {
    super.isAt()

    assertThat(window().title()).isEqualTo(title)
    assertThat(headingText.text()).isEqualTo(heading)
  }
}

class CommunityApiMockServer : WireMockServer(8099)
