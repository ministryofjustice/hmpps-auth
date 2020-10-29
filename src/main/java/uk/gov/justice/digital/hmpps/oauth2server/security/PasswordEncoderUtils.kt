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
package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.crypto.codec.Utf8;

/**
 * Utility for constant time comparison to prevent against timing attacks.
 *
 * @author Rob Winch
 */
class PasswordEncoderUtils {

    /**
     * Constant time comparison to prevent against timing attacks.
     *
     * @param expected String
     * @param actual   String
     * @return true if matches, false otherwise
     */
    static boolean equals(final String expected, final String actual) {
        final var expectedBytes = bytesUtf8(expected);
        final var actualBytes = bytesUtf8(actual);
        final var expectedLength = expectedBytes == null ? -1 : expectedBytes.length;
        final var actualLength = actualBytes == null ? -1 : actualBytes.length;

        var result = expectedLength == actualLength ? 0 : 1;
        for (var i = 0; i < actualLength; i++) {
            final var expectedByte = expectedLength <= 0 ? 0 : expectedBytes[i % expectedLength];
            final var actualByte = actualBytes[i % actualLength];
            result |= expectedByte ^ actualByte;
        }
        return result == 0;
    }

    private static byte[] bytesUtf8(final String s) {
        if (s == null) {
            return null;
        }

        return Utf8.encode(s); // need to check if Utf8.encode() runs in constant time (probably not). This may leak length of string.
    }

    private PasswordEncoderUtils() {
    }
}
