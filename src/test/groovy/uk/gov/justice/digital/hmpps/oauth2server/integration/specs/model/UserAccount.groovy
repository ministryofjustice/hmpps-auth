package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model

import groovy.transform.TupleConstructor

@TupleConstructor
enum UserAccount {

    ITAG_USER('ITAG_USER'),
    ITAG_USER_LOWERCASE('itag_user'),
    ITAG_USER_ADM('ITAG_USER_ADM'),
    NOT_KNOWN('NOT_KNOWN'),
    CA_USER('CA_USER'),
    CA_USER_TEST('CA_USER_TEST'),
    CA_USER_LOWERCASE('ca_user'),
    LOCKED_USER('LOCKED_USER'),
    EXPIRED_USER('EXPIRED_USER'),
    EXPIRED_TEST_USER('EXPIRED_TEST_USER'),
    EXPIRED_TEST2_USER('EXPIRED_TEST2_USER'),
    EXPIRED_TEST3_USER('EXPIRED_TEST3_USER'),
    RESET_TEST_USER('RESET_TEST_USER'),
    RO_USER('RO_USER'),
    RO_DEMO('RO_DEMO'),
    DM_USER('DM_USER'),
    AUTH_USER('AUTH_USER'),
    AUTH_ADM('AUTH_ADM'),
    AUTH_NO_EMAIL('AUTH_NO_EMAIL'),
    AUTH_LOCKED('AUTH_LOCKED'),
    AUTH_LOCKED2('AUTH_LOCKED2'),
    AUTH_DISABLED('AUTH_DISABLED'),
    AUTH_EXPIRED('AUTH_EXPIRED'),
    AUTH_MFA_USER("AUTH_MFA_USER"),
    AUTH_MFA_NOEMAIL_USER("AUTH_MFA_NOEMAIL_USER"),
    AUTH_MFA_EXPIRED_USER("AUTH_MFA_EXPIRED_USER"),
    AUTH_MFA_LOCKED_USER("AUTH_MFA_LOCKED_USER"),
    DELIUS_USER('delius'),
    DELIUS_DISABLED('deldisabled'),
    DELIUS_LOCKED('deliuslocked'),
    DELIUS_PASSWORD_RESET('delius_password_reset'),
    DELIUS_TIMEOUT('deliustimeout'),
    DELIUS_SERVER_ERROR('delius_server_error');

    String username
}
