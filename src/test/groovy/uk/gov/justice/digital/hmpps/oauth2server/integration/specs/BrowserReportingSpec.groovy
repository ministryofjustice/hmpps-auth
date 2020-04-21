package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.junit.Before
import org.junit.ClassRule
import org.openqa.selenium.logging.LogType
import spock.lang.Shared

class BrowserReportingSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TokenVerificationMockServer tokenVerificationServer = new TokenVerificationMockServer()

  @Before
  def "logout to prevent tests interfering with each other"() {
    browser.go("/auth/logout")
  }

  void cleanup() {
    try {
      driver.executeScript("console.log('Test finished')")
      def logEntries = driver.manage().logs().get(LogType.BROWSER).all
      println "START WebDriver $LogType.BROWSER logs"
      logEntries.each {
        println(it)
      }
      println "END WebDriver $LogType.BROWSER logs"
    } catch (error) {
      error.printStackTrace()
    }
  }
}
