package uk.gov.justice.digital.hmpps.oauth2server.azure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.util.ArrayList

internal class AzureUserPersonDetailsTest {

    @Test
    fun `toUser result has correct username`() {
        assertThat(createAzureUser().toUser()?.username).isEqualTo("D6165AD0-AED3-4146-9EF7-222876B57549")
    }

    @Test
    fun `toUser result has correct source`() {
        assertThat(createAzureUser().toUser()?.source).isEqualTo(AuthSource.azuread)
    }

    @Test
    fun `toUser result has correct email`() {
        assertThat(createAzureUser().toUser()?.email).isEqualTo("joe.bloggs@justice.gov.uk")
    }

    @Test
    fun `toUser result is verified`() {
        assertThat(createAzureUser().toUser()?.isVerified).isTrue
    }

    @Test
    fun `toUser result has person with correct firstname`() {
        assertThat(createAzureUser().toUser()?.person?.firstName).isEqualTo("Joe")
    }

    @Test
    fun `toUser result has person with correct lastName`() {
        assertThat(createAzureUser().toUser()?.person?.lastName).isEqualTo("Bloggs")
    }

    private fun createAzureUser() = AzureUserPersonDetails(
            ArrayList(),
            true,
            "D6165AD0-AED3-4146-9EF7-222876B57549",
            "Joe",
            "Bloggs",
            "joe.bloggs@justice.gov.uk",
            true,
            accountNonExpired = true,
            accountNonLocked = true)
}
