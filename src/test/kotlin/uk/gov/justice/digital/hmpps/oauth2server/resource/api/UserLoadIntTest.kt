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
class UserLoadIntTest : IntegrationTest() {
  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `Calling user load creates users`() {
    val output =
      "./user-load.sh localhost:$localServerPort user-load:user-load ITAG_USER 5 src/test/resources/user-load.csv false false"
        .runCommand()
        .split('\n')

    // check 4 rows processed
    assertThat(output.filter { it.startsWith("Processing") })
      .withFailMessage("Was expecting 4 Processing lines, found:\n${output.joinToString("\n")}")
      .hasSize(4)

    // check 3 failures
    assertThat(
      output
        .filter { it.contains("Failure") }
        .map { it.substringAfter("\\033[0;31m").substringBefore("\\033[0m") }
    )
      .withFailMessage("Was expecting 3 Failure lines, found:\n${output.joinToString("\n")}")
      .containsExactly(
        "Failure to create user JOHN.JAMES@SOMEFORCE.PNN.POLICE.UK",
        "Failure to create user JOHN.JAMES@SOMEFORCE.POLICE.UK",
        "Failure to create user AUTH_TEST@DIGITAL.JUSTICE.GOV.UK",
      )

    // check user now loaded successfully
    webTestClient
      .get().uri("/auth/api/user/LOAD_SUCCESS@DIGITAL.JUSTICE.GOV.UK")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it.filter { it.key != "userId" }).containsExactlyInAnyOrderEntriesOf(
          mapOf("username" to "LOAD_SUCCESS@DIGITAL.JUSTICE.GOV.UK", "active" to true, "name" to "Load Success", "authSource" to "auth")
        )
      }

    webTestClient
      .get().uri("/auth/api/authuser/LOAD_SUCCESS@DIGITAL.JUSTICE.GOV.UK/groups")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.[*].groupCode").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrder("PECS_RCHTMC", "PECS_GLDFMC", "PECS_DORKMC", "PECS_SUTTMC", "PECS_RDHLMC")
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
