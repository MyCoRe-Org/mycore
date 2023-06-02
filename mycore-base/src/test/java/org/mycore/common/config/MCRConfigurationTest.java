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

package org.mycore.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCRConfigurationTest extends MCRTestCase {

    @Test(expected = MCRConfigurationException.class)
    public final void testDeprecatedProperties() {
        String deprecatedProperty = "MCR.Editor.FileUpload.MaxSize";
        MCRConfiguration2.getString(deprecatedProperty);
    }

    /**
     * Internal Method of {@link java.util.concurrent.ConcurrentHashMap}.
     * @see {@link java.util.concurrent.ConcurrentHashMap#spread}
     */
    private int spread(int h) {
        return (h ^ (h >>> 16)) & 0x7fffffff /* all usable bits */;
    }

    /**
     * Generate a Singleton Collision and see if it gets correctly resolved.
     * It is limited to 1 minute in case of a recursive operation.
     * <p>This checks for the Bug MCR-2806 - Singleton Recursive Update
     * @author Tobias Lenhardt [Hammer1279]
     */
    @Test(timeout = 3600) // 1 minute
    public final void testSingletonCollision() {

        char[] letters = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        int limit = 50; // initial size of char arrays

        char[][] className = new char[2][limit];
        char[][] property = new char[2][limit];

        int a = -1;
        int b = -1;

        int i = 0;

        // the two test classes
        className[0]
            = "org.mycore.common.config.singletoncollision.MCRConfigurationSingletonCollisionClassA".toCharArray();
        className[1]
            = "org.mycore.common.config.singletoncollision.MCRConfigurationSingletonCollisionClassB".toCharArray();

        // Generate Conflict
        do {
            // resize arrays when full
            if (i == limit) {
                limit = limit * 2;
                for (int j = 0; j < 2; j++) {
                    property[j] = Arrays.copyOf(property[j], limit);
                }
            }

            // Generate Random Text
            property[0][i] = letters[(int) (Math.random() * letters.length)];
            property[1][i] = letters[(int) (Math.random() * letters.length)];

            /*
             * Calculate Slot placement in Map
             * 
             * 1. Step: create a SingletonKey and get its hashCode
             * 2. Step: Spread Bits
             * 3. Step: get Map slot via Bitwise AND
             * 
             * The 15 here is not a magic number.
             * It is to do the calculation assuming we got a size 16 ConcurrentHashMap.
             * Usually right after start, it won't be above this size.
             * Size 16 is the default size for a ConcurrentHashMap.
             */

            a = new MCRConfiguration2.ConfigSingletonKey(String.valueOf(property[0]).trim().replaceAll(" ", ""),
                String.valueOf(className[0]).trim().replaceAll(" ", "")).hashCode();
            a = spread(a);
            a = 15 & a;
            b = new MCRConfiguration2.ConfigSingletonKey(String.valueOf(property[1]).trim().replaceAll(" ", ""),
                String.valueOf(className[1]).trim().replaceAll(" ", "")).hashCode();
            b = spread(b);
            b = 15 & b;
            i++;
        } while (a != b && (a != -1 || b != -1));
        LogManager.getLogger().info("Colliding Strings:");
        LogManager.getLogger().info(String.valueOf(className[0]).trim().replaceAll(" ", "") + " "
            + String.valueOf(property[0]).trim().replaceAll(" ", "") + " => " + a);
        LogManager.getLogger().info(String.valueOf(className[1]).trim().replaceAll(" ", "") + " "
            + String.valueOf(property[1]).trim().replaceAll(" ", "") + " => " + b);
        assertTrue(a == b && (a != -1 || b != -1));

        MCRConfiguration2.set(String.valueOf(property[0]).trim().replaceAll(" ", ""),
            String.valueOf(className[0]).trim().replaceAll(" ", ""));
        MCRConfiguration2.set(String.valueOf(property[1]).trim().replaceAll(" ", ""),
            String.valueOf(className[1]).trim().replaceAll(" ", ""));
        MCRConfiguration2.set("MCR.Configuration.SingletonCollision.PropertyName",
            String.valueOf(property[1]).trim().replaceAll(" ", ""));

        Object classA = MCRConfiguration2.getSingleInstanceOf(String.valueOf(property[0]).trim().replaceAll(" ", ""))
            .orElseThrow();
        assertEquals("MCRConfigurationSingletonCollisionClassA", classA.getClass().getSimpleName());
    }
}
