package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model

import groovy.transform.TupleConstructor

@TupleConstructor
enum UserAccount {

    ITAG_USER('ITAG_USER'),
    NOT_KNOWN('NOT_KNOWN')

    String username
}
