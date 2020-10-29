/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.crypto.codec.Utf8
import kotlin.experimental.xor

/**
 * Utility for constant time comparison to prevent against timing attacks.
 *
 * @author Rob Winch
 */
internal object PasswordEncoderUtils {
  /**
   * Constant time comparison to prevent against timing attacks.
   *
   * @param expected String
   * @param actual   String
   * @return true if matches, false otherwise
   */
  fun equals(expected: String?, actual: String?): Boolean {
    val expectedBytes = bytesUtf8(expected)
    val actualBytes = bytesUtf8(actual)
    val expectedLength = expectedBytes?.size ?: -1
    val actualLength = actualBytes?.size ?: -1
    var result = if (expectedLength == actualLength) 0 else 1

    for (i in 0 until actualLength) {
      val expectedByte = if (expectedLength <= 0) 0 else expectedBytes!![i % expectedLength]
      val actualByte = if (actualLength <= 0) 0 else actualBytes!![i % actualLength]
      result = result or expectedByte.xor(actualByte).toInt()
    }
    return result == 0
  }

  private fun bytesUtf8(s: String?): ByteArray? {
    return if (s == null) {
      null
    } else Utf8.encode(s)
    // need to check if Utf8.encode() runs in constant time (probably not). This may leak length of string.
  }
}
