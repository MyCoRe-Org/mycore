/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.common.function;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;

public class MCRThrowFunctionTest {

    @Test
    public void testToFunctionBiFunctionOfTERClassOfQsuperE() {
        Optional<String> findFirst = Stream.of("passed")
            .map(((MCRThrowFunction<String, String, IOException>) MCRThrowFunctionTest::failExceptionally)
                .toFunction(MCRThrowFunctionTest::mayhandleException, IOException.class))
            .findFirst();
        assertEquals("Junit test:passed", findFirst.get());
    }

    @Test(expected = IOException.class)
    public void testToFunction() throws IOException {
        try {
            Stream.of("passed").map(
                ((MCRThrowFunction<String, String, IOException>) MCRThrowFunctionTest::failExceptionally).toFunction())
                .findFirst();
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public static String failExceptionally(String input) throws IOException {
        throw new IOException("Junit test");
    }

    public static String mayhandleException(String input, IOException e) {
        return e.getMessage() + ":" + input;
    }

}
