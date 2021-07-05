@file:Suppress("DEPRECATION", "SpringJavaInjectionPointsAutowiringInspection")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.OAuth2RequestFactory
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.TokenEnhancer
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ClientRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.UserContextApprovalHandler
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaClientNetworkService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService
import java.security.interfaces.RSAPublicKey
import javax.sql.DataSource

@Configuration
@EnableAuthorizationServer
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class OAuth2AuthorizationServerConfig(
  @Lazy private val authenticationManager: AuthenticationManager,
  @Value("\${jwt.signing.key.pair}") privateKeyPair: String,
  @Value("\${jwt.keystore.password}") private val keystorePassword: String,
  @Value("\${jwt.keystore.alias:elite2api}") private val keystoreAlias: String,
  @Value("\${jwt.jwk.key.id}") private val keyId: String,
  @Qualifier("authDataSource") private val dataSource: DataSource,
  @Lazy private val redirectResolver: RedirectResolver,
  private val passwordEncoder: PasswordEncoder,
  private val telemetryClient: TelemetryClient,
  @Qualifier("tokenVerificationApiRestTemplate") private val restTemplate: RestTemplate,
  @Value("\${tokenverification.enabled:false}") private val tokenVerificationEnabled: Boolean,
  private val tokenVerificationClientCredentials: TokenVerificationClientCredentials,
  private val userContextService: UserContextService,
  private val mfaClientNetworkService: MfaClientNetworkService,
  private val clientRepository: ClientRepository,
) : AuthorizationServerConfigurerAdapter() {

  private val privateKeyPair: Resource = ByteArrayResource(Base64.decodeBase64(privateKeyPair))

  private var jdbcClientDetailsService: JdbcClientDetailsService? = null
  private var tokenStore: JwtTokenStore? = null
  private var oAuth2RequestFactory: DefaultOAuth2RequestFactory? = null
  private var mfaClientService: MfaClientService? = null

  companion object {
    private const val HOUR_IN_SECS = 60 * 60
  }

  @Bean
  fun tokenStore(): TokenStore? {
    if (tokenStore == null) {
      tokenStore = JwtTokenStore(accessTokenConverter())
    }
    return tokenStore
  }

  @Bean
  @Primary
  fun jdbcClientDetailsService(): JdbcClientDetailsService {
    if (jdbcClientDetailsService == null) {
      jdbcClientDetailsService = JdbcClientDetailsService(dataSource)
      jdbcClientDetailsService!!.setPasswordEncoder(passwordEncoder)
    }
    return jdbcClientDetailsService!!
  }

  @Throws(Exception::class)
  override fun configure(clients: ClientDetailsServiceConfigurer) {
    clients.withClientDetails(jdbcClientDetailsService())
  }

  @Bean
  fun accessTokenConverter(): JwtAccessTokenConverter {
    val keyStoreKeyFactory = KeyStoreKeyFactory(privateKeyPair, keystorePassword.toCharArray())
    return JwtKeyIdHeaderAccessTokenConverter(keyId, keyStoreKeyFactory.getKeyPair(keystoreAlias))
  }

  override fun configure(oauthServer: AuthorizationServerSecurityConfigurer) {
    oauthServer.addAuthenticationProvider(UrlDecodingRetryDaoAuthenticationProvider(telemetryClient, jdbcClientDetailsService(), passwordEncoder))
    oauthServer.tokenKeyAccess("permitAll()")
      .checkTokenAccess("isAuthenticated()")
  }

  @Bean
  fun jwtTokenEnhancer(): TokenEnhancer = JWTTokenEnhancer()

  override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
    endpoints.tokenStore(tokenStore())
      .accessTokenConverter(accessTokenConverter())
      .tokenEnhancer(tokenEnhancerChain())
      .redirectResolver(redirectResolver)
      .authenticationManager(authenticationManager)
      .authorizationCodeServices(JdbcAuthorizationCodeServices(dataSource))
      .requestFactory(requestFactory())
      .userApprovalHandler(userApprovalHandler())
      .tokenServices(tokenServices())
  }

  @Bean
  fun mfaClientService(): MfaClientService {
    if (mfaClientService == null) {
      mfaClientService = MfaClientService(jdbcClientDetailsService(), mfaClientNetworkService)
    }
    return mfaClientService!!
  }

  private fun userApprovalHandler(): UserApprovalHandler {
    val approvalHandler =
      UserContextApprovalHandler(userContextService, jdbcClientDetailsService(), mfaClientService())
    approvalHandler.setRequestFactory(requestFactory())
    approvalHandler.setTokenStore(tokenStore())
    return approvalHandler
  }

  private fun requestFactory(): OAuth2RequestFactory? {
    if (oAuth2RequestFactory == null) {
      oAuth2RequestFactory = DefaultOAuth2RequestFactory(jdbcClientDetailsService())
    }
    return oAuth2RequestFactory
  }

  @Bean
  fun tokenEnhancerChain(): TokenEnhancerChain {
    val tokenEnhancerChain = TokenEnhancerChain()
    tokenEnhancerChain.setTokenEnhancers(listOf(jwtTokenEnhancer(), accessTokenConverter()))
    return tokenEnhancerChain
  }

  @Bean
  @Primary
  fun tokenServices(): DefaultTokenServices {
    val tokenServices =
      TrackingTokenServices(telemetryClient, restTemplate, clientRepository, tokenVerificationClientCredentials, tokenVerificationEnabled)
    tokenServices.setTokenEnhancer(tokenEnhancerChain())
    tokenServices.setTokenStore(tokenStore())
    tokenServices.setReuseRefreshToken(true)
    tokenServices.setSupportRefreshToken(true)
    tokenServices.setAccessTokenValiditySeconds(HOUR_IN_SECS) // default 1 hours
    tokenServices.setRefreshTokenValiditySeconds(HOUR_IN_SECS * 12) // default 12 hours
    tokenServices.setClientDetailsService(jdbcClientDetailsService())
    tokenServices.setAuthenticationManager(authenticationManager)
    return tokenServices
  }

  @Bean
  fun jwkSet(): JWKSet {
    val keyStoreKeyFactory = KeyStoreKeyFactory(privateKeyPair, keystorePassword.toCharArray())
    val builder = RSAKey.Builder(keyStoreKeyFactory.getKeyPair(keystoreAlias).public as RSAPublicKey)
      .keyUse(KeyUse.SIGNATURE)
      .algorithm(JWSAlgorithm.RS256)
      .keyID(keyId)
    return JWKSet(builder.build())
  }

  @Bean
  fun authExpressionHandler(applicationContext: ApplicationContext): OAuth2WebSecurityExpressionHandler {
    val expressionHandler = OAuth2WebSecurityExpressionHandler()
    expressionHandler.setApplicationContext(applicationContext)
    return expressionHandler
  }
}
