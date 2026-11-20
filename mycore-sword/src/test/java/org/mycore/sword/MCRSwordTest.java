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

package org.mycore.sword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mycore.common.config.MCRConfigurationException;

public class MCRSwordTest {

    private static final String PROVIDER = "org.example.Provider";

    @Test
    public void testCollectionWithClassSuffix() {
        Map<String, String> properties = Map.of(
            "MCR.Sword.Collection.WS.Col.Class", PROVIDER,
            "MCR.Other.Property", "value");

        assertEquals(Set.of("WS.Col"), MCRSword.parseCollectionNames(properties));
    }

    @Test
    public void testNestedPropertiesAreNoCollections() {
        Map<String, String> properties = Map.of(
            "MCR.Sword.Collection.WS.Col.Class", PROVIDER,
            "MCR.Sword.Collection.WS.Col.Foo", "bar");

        assertEquals(Set.of("WS.Col"), MCRSword.parseCollectionNames(properties));
    }

    @Test
    public void testLegacyFormatThrows() {
        Map<String, String> properties = Map.of(
            "MCR.Sword.Collection.WS.Col", PROVIDER);

        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRSword.parseCollectionNames(properties));
        assertTrue(exception.getMessage()
            .contains("MCR.Sword.Collection.WS.Col ==> MCR.Sword.Collection.WS.Col.Class"));
    }

    @Test
    public void testClassWithoutWorkspaceAndCollectionThrows() {
        Map<String, String> properties = Map.of(
            "MCR.Sword.Collection.Class", PROVIDER);

        assertThrows(MCRConfigurationException.class, () -> MCRSword.parseCollectionNames(properties));
    }

}
