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

package org.mycore.datamodel.classifications2;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRLabelTest extends MCRTestCase {

    @Test
    public final void testMCRLabelStringStringString() {
        MCRLabel de = new MCRLabel("de", "test", null);
        try {
            MCRLabel de2 = new MCRLabel("de", null, null);
            fail("MCRLabel should not allow 'null' as 'text'.");
        } catch (NullPointerException e) {
        }
        try {
            MCRLabel de3 = new MCRLabel("de", "", null);
            fail("MCRLabel should not allow empty 'text'.");
        } catch (IllegalArgumentException e) {
        }
        try {
            MCRLabel de4 = new MCRLabel("de", " ", null);
        } catch (IllegalArgumentException e) {
        }
        MCRLabel xUri = new MCRLabel("x-uri", "http://...", null);
        try {
            MCRLabel xxl = new MCRLabel("x-toolong", "http://...", null);
        } catch (IllegalArgumentException e) {
        }
    }

}
