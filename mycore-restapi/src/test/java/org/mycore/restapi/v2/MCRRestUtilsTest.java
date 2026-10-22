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

package org.mycore.restapi.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MCRRestUtilsTest {

    @Test
    public void getContentDispositionKeepsAsciiFileName() {
        assertEquals("attachment; filename=\"mir_derivate_00000001.zip\"; "
            + "filename*=UTF-8''mir_derivate_00000001.zip",
            MCRRestUtils.getContentDisposition("attachment", "mir_derivate_00000001.zip"));
    }

    @Test
    public void getContentDispositionEncodesNonAsciiCharacters() {
        assertEquals("attachment; filename=\"mir_derivate_00000001-B_cher.zip\"; "
            + "filename*=UTF-8''mir_derivate_00000001-B%C3%BCcher.zip",
            MCRRestUtils.getContentDisposition("attachment", "mir_derivate_00000001-Bücher.zip"));
    }

    @Test
    public void getContentDispositionEscapesHeaderSyntax() {
        assertEquals("inline; filename=\"a _b_ c_d;e*.tar\"; filename*=UTF-8''a%20%22b%22%20c%5Cd%3Be%2A.tar",
            MCRRestUtils.getContentDisposition("inline", "a \"b\" c\\d;e*.tar"));
    }

}
