package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.openqa.selenium.logging.LogType

class BrowserReportingSpec extends GebReportingSpec {
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
