package uk.gov.justice.digital.hmpps.oauth2server.service

import uk.gov.service.notify.NotificationClientException

class NotificationClientRuntimeException(e: NotificationClientException?) : RuntimeException(e)
