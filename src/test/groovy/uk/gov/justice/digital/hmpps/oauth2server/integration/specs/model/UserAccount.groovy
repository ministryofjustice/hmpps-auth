package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model

import groovy.transform.TupleConstructor

@TupleConstructor
enum UserAccount {

    ITAG_USER('ITAG_USER'),
    ITAG_USER_LOWERCASE('itag_user'),
    ITAG_USER_ADM('ITAG_USER_ADM'),
    NOT_KNOWN('NOT_KNOWN'),
    CA_USER('CA_USER'),
    CA_USER_LOWERCASE('ca_user'),
    LOCKED_USER('LOCKED_USER'),
    EXPIRED_USER('EXPIRED_USER'),
    EXPIRED_TEST_USER('EXPIRED_TEST_USER'),
    EXPIRED_TEST2_USER('EXPIRED_TEST2_USER'),
    EXPIRED_TEST3_USER('EXPIRED_TEST3_USER'),
    RO_USER('RO_USER'),
    RO_DEMO('RO_DEMO'),
    DM_USER('DM_USER'),
    AUTH_ONLY_USER('AUTH_ONLY_USER')

    String username
}
