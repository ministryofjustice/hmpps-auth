package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorityTest {
    @Test
    void testConstructor_addsRole() {
        assertThat(new Authority("BOB", "bloggs").getAuthority()).isEqualTo("ROLE_BOB");
    }

    @Test
    void testConstructor_unecessary() {
        assertThat(new Authority("ROLE_BOB", "bloggs").getAuthority()).isEqualTo("ROLE_BOB");
    }

    @Test
    void getAuthorityName() {
        assertThat(new Authority("ROLE_BOB", "bloggs").getRoleCode()).isEqualTo("BOB");
    }

    @Test
    void removeRolePrefixIfNecessary_necessary() {
        assertThat(Authority.removeRolePrefixIfNecessary("ROLE_BOB")).isEqualTo("BOB");
    }

    @Test
    void removeRolePrefixIfNecessary_unnecessary() {
        assertThat(Authority.removeRolePrefixIfNecessary("BOB")).isEqualTo("BOB");
    }

    @Test
    void removeRolePrefixIfNecessary_null() {
        assertThat(Authority.removeRolePrefixIfNecessary(null)).isEqualTo(null);
    }
}
