package uk.gov.justice.digital.hmpps.oauth2server.service

import uk.gov.service.notify.NotificationClientException

// Throttling doesn't allow checked exceptions to be thrown, hence wrapped in a runtime exception
class NotificationClientRuntimeException(e: NotificationClientException?) : RuntimeException(e)
