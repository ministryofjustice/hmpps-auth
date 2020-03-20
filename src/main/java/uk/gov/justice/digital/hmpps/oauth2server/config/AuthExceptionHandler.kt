package uk.gov.justice.digital.hmpps.oauth2server.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail


@RestControllerAdvice
class AuthExceptionHandler {
  @ExceptionHandler(UsernameNotFoundException::class)
  fun handleNotFoundException(e: Exception): ResponseEntity<ErrorDetail> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorDetail(HttpStatus.NOT_FOUND.reasonPhrase, e.message ?: "Error message not set", "username"))
  }

  companion object {
    private val log = LoggerFactory.getLogger(AuthExceptionHandler::class.java)
  }
}
