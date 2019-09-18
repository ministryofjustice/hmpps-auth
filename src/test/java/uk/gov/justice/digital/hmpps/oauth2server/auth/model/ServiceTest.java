package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceTest {

    @Test
    public void getRoles() {
        final var service = new Service("CODE", "NAME", "Description", "SOME_ROLE, SOME_OTHER_ROLE", "http://some.url", true, "a@b.com");
        assertThat(service.getRoles()).containsExactly("SOME_ROLE", "SOME_OTHER_ROLE");
    }

    @Test
    public void getRoles_empty() {
        final var service = new Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com");
        assertThat(service.getRoles()).isEmpty();
    }

    @Test
    public void isUrlInsteadOfEmail() {
        final var service = new Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com");
        assertThat(service.isUrlInsteadOfEmail()).isFalse();
    }

    @Test
    public void isUrlInsteadOfEmail_true() {
        final var service = new Service("CODE", "NAME", "Description", null, "http://some.url", true, "http://some.url");
        assertThat(service.isUrlInsteadOfEmail()).isTrue();
    }
}
