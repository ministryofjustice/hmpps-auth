package uk.gov.justice.digital.hmpps.oauth2server.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleExistsException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.GroupsService.GroupNotFoundException
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException

@RestControllerAdvice
class AuthExceptionHandler {
  @ExceptionHandler(UsernameNotFoundException::class)
  fun handleNotFoundException(e: UsernameNotFoundException): ResponseEntity<ErrorDetail> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorDetail(HttpStatus.NOT_FOUND.reasonPhrase, e.message ?: "Error message not set", "username"))
  }

  @ExceptionHandler(AuthUserRoleExistsException::class)
  fun handleAuthUserRoleExistsException(e: AuthUserRoleExistsException): ResponseEntity<ErrorDetail> {
    log.debug("Auth user role exists exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorDetail(e.errorCode, e.message ?: "Error message not set", e.field))
  }

  @ExceptionHandler(AuthUserGroupRelationshipException::class)
  fun handleAuthUserGroupRelationshipException(e: AuthUserGroupRelationshipException): ResponseEntity<ErrorDetail> {
    log.debug("Auth user group relationship exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorDetail(e.errorCode, e.message ?: "Error message not set", "username"))
  }

  @ExceptionHandler(GroupNotFoundException::class)
  fun GroupNotFoundException(e: GroupNotFoundException): ResponseEntity<ErrorDetail> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorDetail(HttpStatus.NOT_FOUND.reasonPhrase, e.message ?: "Error message not set", "group"))
  }

  @ExceptionHandler(AuthGroupRelationshipException::class)
  fun handleAuthGroupRelationshipException(e: AuthGroupRelationshipException): ResponseEntity<ErrorDetail> {
    log.debug("Auth maintain group relationship exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorDetail(e.errorCode, e.message ?: "Error message not set", "group"))
  }

  @ExceptionHandler(AuthUserRoleException::class)
  fun handleAuthUserRoleException(e: AuthUserRoleException): ResponseEntity<ErrorDetail> {
    log.debug("Auth user role exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorDetail(e.errorCode, e.message ?: "Error message not set", e.field))
  }

  companion object {
    private val log = LoggerFactory.getLogger(AuthExceptionHandler::class.java)
  }
}
