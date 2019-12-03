package uk.gov.justice.digital.hmpps.oauth2server.integration.specs


import org.junit.Rule
import uk.gov.justice.digital.hmpps.oauth2server.integration.wiremock.CommunityApiMockServer

@SuppressWarnings("GrDeprecatedAPIUsage")
class DeliusIntegrationSpec extends BrowserReportingSpec {
  @Rule
  CommunityApiMockServer communityApi = new CommunityApiMockServer()
}
