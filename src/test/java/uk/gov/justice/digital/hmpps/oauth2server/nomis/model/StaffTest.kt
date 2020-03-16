package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaffTest {

    @Test
    void getFirstName() {
        final var staff = new Staff();
        staff.setFirstName("boB");
        assertThat(staff.getFirstName()).isEqualTo("Bob");
    }

    @Test
    void getName() {
        final var staff = new Staff();
        staff.setFirstName("boB");
        staff.setLastName("SMITH");
        assertThat(staff.getName()).isEqualTo("Bob Smith");
    }
}
