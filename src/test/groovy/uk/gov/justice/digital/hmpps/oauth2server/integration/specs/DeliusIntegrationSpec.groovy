package uk.gov.justice.digital.hmpps.oauth2server.integration.specs


import org.junit.Rule

@SuppressWarnings("GrDeprecatedAPIUsage")
class DeliusIntegrationSpec extends BrowserReportingSpec {
  @Rule
  CommunityApiMockServer communityApi = new CommunityApiMockServer()
}
