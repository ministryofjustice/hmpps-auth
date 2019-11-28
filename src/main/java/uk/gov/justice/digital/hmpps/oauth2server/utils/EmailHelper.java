package uk.gov.justice.digital.hmpps.oauth2server.utils;

import static org.apache.commons.lang3.StringUtils.*;

public class EmailHelper {
    public static String format(final String emailInput) {
        return replaceChars(lowerCase(trim(emailInput)), '’', '\'');
    }
}