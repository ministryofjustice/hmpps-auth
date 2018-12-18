package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReferenceDomain {
    EMAIL_DOMAIN("EMAIL_DOMAIN");

    private final String domain;
}
