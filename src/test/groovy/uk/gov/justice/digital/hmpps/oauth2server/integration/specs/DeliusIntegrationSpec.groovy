package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import org.junit.ClassRule
import spock.lang.Shared

@SuppressWarnings("GrDeprecatedAPIUsage")
class DeliusIntegrationSpec extends BrowserReportingSpec {
  @Shared
  @ClassRule
  CommunityApiMockServer communityApi = new CommunityApiMockServer()
}
