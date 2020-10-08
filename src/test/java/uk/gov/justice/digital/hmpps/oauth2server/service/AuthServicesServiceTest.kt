package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import java.util.Optional
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

class AuthServicesServiceTest {
  private val oauthServiceRepository: OauthServiceRepository = mock()
  private val authServicesService = AuthServicesService(oauthServiceRepository)

  @Test
  fun `list calls repository find all`() {
    val services = mutableListOf(Service())
    whenever(oauthServiceRepository.findAllByOrderByName()).thenReturn(services)
    assertThat(authServicesService.list()).isSameAs(services)
    verify(oauthServiceRepository).findAllByOrderByName()
  }

  @Test
  fun `get service finds service`() {
    val service = Service()
    whenever(oauthServiceRepository.findById(anyString())).thenReturn(Optional.of(service))
    assertThat(authServicesService.getService("code")).isSameAs(service)
    verify(oauthServiceRepository).findById("code")
  }

  @Test
  fun `get service throws exception if not found`() {
    assertThatThrownBy { authServicesService.getService("code") }
      .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Entity code not found")
    verify(oauthServiceRepository).findById("code")
  }

  @Test
  fun `update calls save`() {
    val service = Service()
    authServicesService.updateService(service)
    verify(oauthServiceRepository).save<Service>(
      check {
        assertThat(it).isSameAs(service)
      }
    )
  }

  @Test
  fun `add calls save`() {
    val service = Service()
    authServicesService.addService(service)
    verify(oauthServiceRepository).save<Service>(
      check {
        assertThat(it).isSameAs(service)
      }
    )
  }

  @Test
  fun `add checks service doesn't already exist`() {
    val service = Service()
    service.code = "newcode"
    whenever(oauthServiceRepository.findById(anyString())).thenReturn(Optional.of(Service()))
    assertThatThrownBy { authServicesService.addService(service) }
      .isInstanceOf(EntityExistsException::class.java).hasMessage("Entity newcode already exists")

    verify(oauthServiceRepository).findById("newcode")
    verifyNoMoreInteractions(oauthServiceRepository)
  }

  @Test
  fun `remove calls delete`() {
    authServicesService.removeService("code")
    verify(oauthServiceRepository).deleteById("code")
  }
}
