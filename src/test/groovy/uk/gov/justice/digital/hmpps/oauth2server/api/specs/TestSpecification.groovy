package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.TokenVerificationMockServer

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SuppressWarnings("GrDeprecatedAPIUsage")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
@Slf4j
abstract class TestSpecification extends Specification {
  @Rule
  TokenVerificationMockServer tokenVerificationServer = new TokenVerificationMockServer()

  @Autowired
  OAuth2RestTemplate tokenVerificationApiRestTemplate

  @LocalServerPort
  int randomServerPort;

  @Rule
  TestWatcher t = new TestWatcher() {
    @Override
    protected void starting(Description description) {
      log.info("Starting test '{}'", description.getDisplayName())
    }

        @Override
        protected void finished(Description description) {
          log.info("Finished test '{}'", description.getDisplayName())
        }
  }

  @Autowired
  TestRestTemplate restTemplate

  @Autowired
  ObjectMapper objectMapper

  @BeforeClass
  static void setupClass() {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  void setup() {
    // need to override port as random port only assigned on server startup
    tokenVerificationApiRestTemplate.getResource().accessTokenUri = "http://localhost:${randomServerPort}/auth/oauth/token"
  }

  HttpEntity createHeaderEntity(Object entity) {
    HttpHeaders headers = new HttpHeaders()
    headers.add("Authorization", "bearer " + token)
    headers.setContentType(MediaType.APPLICATION_JSON)
    new HttpEntity<>(entity, headers)
  }

  OAuth2RestTemplate getOauthPasswordGrant(String username, String password, String clientId, String clientSecret) {
    authenticate(ownerPasswordResource(username, password, clientId, clientSecret), null)
    }

    OAuth2RestTemplate getOauthClientGrant(String clientId, String clientSecret) {
        authenticate(clientCredentialsResource(clientId, clientSecret), null)
    }

    OAuth2RestTemplate getOauthClientGrant(String clientId, String clientSecret, String queryStr) {
        authenticate(clientCredentialsResource(clientId, clientSecret), queryStr)
    }

    OAuth2AccessToken refresh(OAuth2RestTemplate template) {
        ResourceOwnerPasswordAccessTokenProvider refresh = new ResourceOwnerPasswordAccessTokenProvider()
        refresh.refreshAccessToken(template.getResource(), template.getAccessToken().getRefreshToken(), new DefaultAccessTokenRequest())
    }

    private OAuth2RestTemplate authenticate(BaseOAuth2ProtectedResourceDetails resource, String queryStr) {
        resource.setAccessTokenUri(accessTokenUri() + (StringUtils.isNotBlank(queryStr) ? "?" + queryStr : ""));
        new OAuth2RestTemplate(resource, new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest()))
    }

    private static BaseOAuth2ProtectedResourceDetails ownerPasswordResource(String username, String password, String clientId, String clientSecret) {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails()
        resource.setClientId(clientId)
        resource.setClientSecret(clientSecret)
        resource.setUsername(username)
        resource.setPassword(password)
        resource
    }

    private static BaseOAuth2ProtectedResourceDetails clientCredentialsResource(String clientId, String clientSecret) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails()
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource
    }

    private String accessTokenUri() {
        getBaseUrl() + "/oauth/token"
    }

    protected String getBaseUrl() {
        "http://localhost:" + randomServerPort + "/auth"
    }
}
