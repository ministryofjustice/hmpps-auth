package uk.gov.justice.digital.hmpps.oauth2server.azure.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.util.Collections
import java.util.Optional

internal class AzureUserServiceTest {

  private val mockUserRepository: UserRepository = mock()
  private val azureUserService: AzureUserService = AzureUserService(mockUserRepository)

  @Test
  fun getAzureUserByUsername() {

    whenever(
      mockUserRepository
        .findByUsernameAndSource("917D4BDC-F86F-4756-B828-0BED8865EFB3", AuthSource.azuread)
    )
      .thenReturn(
        Optional.of(
          createSampleUser(
            username = "917D4BDC-F86F-4756-B828-0BED8865EFB3",
            firstName = "Test",
            lastName = "User",
            email = "test@user.com",
          )
        )
      )

    val azureUser = azureUserService.getAzureUserByUsername("917D4BDC-F86F-4756-B828-0BED8865EFB3")

    assertThat(azureUser).isEqualTo(
      Optional.of(
        AzureUserPersonDetails(
          Collections.emptyList(),
          true,
          "917D4BDC-F86F-4756-B828-0BED8865EFB3",
          "Test",
          "User",
          "test@user.com",
          credentialsNonExpired = true,
          accountNonExpired = true,
          accountNonLocked = true
        )
      )
    )
  }
}
