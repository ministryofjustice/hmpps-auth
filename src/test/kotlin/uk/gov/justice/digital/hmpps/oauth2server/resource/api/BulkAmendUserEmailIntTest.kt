package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(DeliusExtension::class)
class BulkAmendUserEmailIntTest : IntegrationTest() {
  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `Calling amend user email script amends user emails`() {
    val output =
      "./amend-email.sh localhost:$localServerPort user-load:user-load ITAG_USER 5 src/test/resources/amend-email.csv"
        .runCommand()
        .split('\n')

    // check 5 rows amended
    assertThat(output.filter { it.startsWith("Amending") })
      .withFailMessage("Was expecting 5 Amending lines, found:\n${output.joinToString("\n")}")
      .hasSize(5)

    // check 4 failures
    assertThat(
      output.filter { it.contains("Failed") }
        .map { it.substringBefore(" due to") }
    )
      .withFailMessage("Was expecting 3 Failure lines, found:\n${output.joinToString("\n")}")
      .containsExactly(
        "Failed to amend email for NO_USER_FOUND_HERE to john.james@someforce.police.uk",
        "Failed to amend email for AUTH_USER to john.james@someforce.pnn.police.uk",
        "Failed to amend email for AUTH_RO_USER1@DIGITAL.JUSTICE.GOV.UK to auth_user_email@justice.gov.uk",
      )

    // check user now amended successfully
    webTestClient
      .get().uri("/api/user/AUTH_BULK_AMEND_EMAIL")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf("username" to "AUTH_BULK_AMEND_EMAIL", "active" to true, "name" to "User Change Test", "authSource" to "auth")
        )
      }

    // check second user now amended successfully
    webTestClient
      .get().uri("/api/user/auth_bulk_amend_email2@digital.justice.gov.uk")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf("username" to "AUTH_BULK_AMEND_EMAIL2@DIGITAL.JUSTICE.GOV.UK", "active" to true, "name" to "User Change Test", "authSource" to "auth")
        )
      }
  }

  private fun String.runCommand(
    workingDir: File = File("."),
    timeoutDuration: Duration = Duration.ofMinutes(1L),
  ): String =

    ProcessBuilder(split("\\s".toRegex()))
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start().apply { waitFor(timeoutDuration.seconds, TimeUnit.SECONDS) }
      .inputStream.bufferedReader().readText()
}
