package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension

@ExtendWith(DeliusExtension::class)
open class AbstractDeliusAuthSpecification : AbstractAuthSpecification()
