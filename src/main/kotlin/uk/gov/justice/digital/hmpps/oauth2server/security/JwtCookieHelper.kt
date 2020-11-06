package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.oauth2server.utils.CookieHelper

@Component
class JwtCookieHelper(properties: JwtCookieConfigurationProperties) : CookieHelper(properties.name, properties.expiryTime)
