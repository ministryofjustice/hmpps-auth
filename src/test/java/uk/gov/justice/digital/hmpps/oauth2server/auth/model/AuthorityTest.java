package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorityTest {
    @Test
    public void testConstructor_addsRole() {
        assertThat(new Authority("BOB").getAuthority()).isEqualTo("ROLE_BOB");
    }

    @Test
    public void testConstructor_unecessary() {
        assertThat(new Authority("ROLE_BOB").getAuthority()).isEqualTo("ROLE_BOB");
    }

    @Test
    public void getAuthorityName() {
        assertThat(new Authority("ROLE_BOB").getAuthorityName()).isEqualTo("BOB");
    }

    @Test
    public void addRolePrefixIfNecessary_necessary() {
        assertThat(Authority.addRolePrefixIfNecessary("BOB")).isEqualTo("ROLE_BOB");
    }

    @Test
    public void addRolePrefixIfNecessary_unnecessary() {
        assertThat(Authority.addRolePrefixIfNecessary("ROLE_BOB")).isEqualTo("ROLE_BOB");
    }

    @Test
    public void addRolePrefixIfNecessary_null() {
        assertThat(Authority.addRolePrefixIfNecessary(null)).isEqualTo("ROLE_null");
    }
}
