/*
 * $Id$
 * $Revision: 5697 $ $Date: 14.01.2010 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.fileupload;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@RunWith(Parameterized.class)
public class MCRUploadHelperTest extends MCRTestCase {
    static String prefix = "junit";

    static String suffix = "test..file";

    static String[] genDelims = new String[] { ":", "?", "#", "[", "]", "@" };

    static String[] subDelims = new String[] { "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=" };

    private static final String[] WINDOWS_RESERVED_CHARS = { "<", ">", ":", "\"", "|", "?", "*" };

    private static final String[] RESERVED_NAMES = { "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8",
        "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con", "nul", "prn", "aux" };

    @Parameter(0)
    public String path;

    @Parameter(1)
    public Class<? extends Exception> expectedException;

    @Parameters
    public static Iterable<Object[]> data() {
        return Stream.concat(toParameter(validNames(), null),
            toParameter(invalidNames(), MCRException.class))::iterator;
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

    @Test
    public void checkPathName() {
        try {
            MCRUploadHelper.checkPathName(path);
            if (expectedException != null) {
                throw new AssertionError(
                    "Expected test to throw instance of " + expectedException.getName() + " for path=\"" + path + "\"");
            }
        } catch (RuntimeException e) {
            if (expectedException == null || !expectedException.isAssignableFrom(e.getClass())) {
                throw e;
            }
        }
    }

}
