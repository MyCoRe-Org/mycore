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

package org.mycore.mcr.acl.accesskey.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;

public class MCRNullableTest {

    private static final String TEST_VALUE = "testValue";

    @Test
    public void testNullable_present() {
        final MCRNullable<String> nullable = new MCRNullable<>(TEST_VALUE);
        assertTrue(nullable.isPresent());
        assertNotNull(nullable.get());
        assertEquals(TEST_VALUE, nullable.get());
        assertTrue(nullable.getOptional().isPresent());
        assertEquals(TEST_VALUE, nullable.getOptional().get());
    }

    @Test
    public void testNullable_empty() {
        final MCRNullable<String> nullable = new MCRNullable<>();
        assertFalse(nullable.isPresent());
        assertNull(nullable.get());
        assertTrue(nullable.getOptional().isEmpty());
    }

    @Test
    public void testNullable_null() {
        final MCRNullable<String> nullable = new MCRNullable<>(null);
        assertTrue(nullable.isPresent());
        assertNull(nullable.get());
        assertTrue(nullable.getOptional().isEmpty());
    }
}
