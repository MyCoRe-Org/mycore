/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.fileupload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
public class MCRUploadHelperTest {
    static String prefix = "junit";

    static String suffix = "test..file";

    static String[] genDelims = { ":", "?", "#", "[", "]", "@" };

    static String[] subDelims = { "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=" };

    private static final String[] WINDOWS_RESERVED_CHARS = { "<", ">", ":", "\"", "|", "?", "*", "\\" };

    private static final String[] RESERVED_NAMES = { "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8",
        "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con", "nul", "prn", "aux" };

    static Stream<Object[]> testData() {
        return Stream.concat(toParameter(validNames(), null),
            toParameter(invalidNames(), MCRException.class));
    }

    private static Stream<String> invalidNames() {
        return MCRStreamUtils.concat(
            Stream.of(RESERVED_NAMES),
            validNames().map(s -> "../" + s),
            validNames().map(s -> "./" + s),
            validNames().map(s -> "/" + s),
            validNames().map(s -> "foo//" + s),
            MCRStreamUtils.concat(IntStream.range(0, 32).mapToObj(i -> "" + (char) i),
                Stream.of(WINDOWS_RESERVED_CHARS),
                Stream.concat(Stream.of(genDelims), Stream.of(subDelims)).distinct().map(c -> prefix + c + suffix)));
    }

    private static Stream<String> validNames() {
        return Stream.of(prefix + " " + suffix, prefix + suffix, "." + prefix + suffix);
    }

    private static Stream<Object[]> toParameter(Stream<String> input, Class<? extends Exception> exp) {
        return input.map(s -> new Object[] { s, exp });
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void checkPathName(String path, Class<? extends Exception> expectedException) {
        if (expectedException == null) {
            assertDoesNotThrow(() -> MCRUploadHelper.checkPathName(path));
        } else {
            assertThrows(expectedException, () -> MCRUploadHelper.checkPathName(path));
        }
    }
}
