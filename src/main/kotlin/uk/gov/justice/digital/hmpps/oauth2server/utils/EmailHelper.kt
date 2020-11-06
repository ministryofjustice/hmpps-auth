package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.apache.commons.lang3.StringUtils

object EmailHelper {
  @JvmStatic
  fun format(emailInput: String?): String? =
    StringUtils.replaceChars(StringUtils.lowerCase(StringUtils.trim(emailInput)), '’', '\'')
}
