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

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2.SingletonKey;
import org.mycore.common.config.singletoncollision.MCRConfigurationSingletonCollisionClassA;
import org.mycore.common.config.singletoncollision.MCRConfigurationSingletonCollisionClassB;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Scheffler (yagee)
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCRConfigurationTest extends MCRTestCase {
    
    public static final String COLLISION_PROPERTY_NAME =  "MCR.Configuration.SingletonCollision.PropertyName";

    @Test(expected = MCRConfigurationException.class)
    public final void testDeprecatedProperties() {
        String deprecatedProperty = "MCR.Editor.FileUpload.MaxSize";
        MCRConfiguration2.getString(deprecatedProperty);
    }

    /**
     * Generate a Singleton Collision and see if it gets correctly resolved.
     * It is limited to 1 minute in case of a recursive operation.
     * <p>This checks for the Bug MCR-2806 - Singleton Recursive Update
     *
     * @author Tobias Lenhardt [Hammer1279]
     */
    @Test(timeout = 3600) // 1 minute
    public final void testSingletonCollision() {

        Pair<String, String> properties = createConflictConfiguration();

        Object objectA = MCRConfiguration2.getSingleInstanceOf(properties.getLeft()).orElseThrow();
        Object objectB = MCRConfigurationSingletonCollisionClassA.COLLISION_CLASS_B;

        assertTrue("Wrong or no class loaded!", objectA instanceof MCRConfigurationSingletonCollisionClassA);
        assertTrue("Wrong or no class loaded!", objectB instanceof MCRConfigurationSingletonCollisionClassB);

    }

    /**
     * <p>MCR-2827 check for correct get behavior</p>
     * Reuses the collision mapping created for {@link #testSingletonCollision}, so if it fails this is also likely to 
     * fail.
     *
     * @author Tobias Lenhardt [Hammer1279]
     */
    @Test
    public final void testSingletonMapGet() {

        Pair<String, String> properties = createConflictConfiguration();
        String propertyA = properties.getLeft();
        String propertyB = properties.getRight();
        String classNameA = MCRConfiguration2.getStringOrThrow(propertyA);
        String classNameB = MCRConfiguration2.getStringOrThrow(propertyB);

        MCRConfiguration2.getSingleInstanceOf(propertyA).orElseThrow();

        assertNotNull(MCRConfiguration2.instanceHolder.get(new SingletonKey(propertyA, classNameA)));
        assertNotNull(MCRConfiguration2.instanceHolder.get(new SingletonKey(propertyB, classNameB)));

    }

    private Pair<String, String> createConflictConfiguration() {

        char[] letters = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        int limit = 50; // initial size of char arrays

        String classNameA = MCRConfigurationSingletonCollisionClassA.class.getName();
        String classNameB = MCRConfigurationSingletonCollisionClassB.class.getName();

        char[] propertyCharsA = new char[limit];
        char[] propertyCharsB = new char[limit];

        String propertyA;
        String propertyB;

        int a;
        int b;
        
        int i = 0;
        
        // Generate Conflict
        do {
            // resize arrays when full
            if (i == limit) {
                limit = limit * 2;
                propertyCharsA = Arrays.copyOf(propertyCharsA, limit);
                propertyCharsB = Arrays.copyOf(propertyCharsB, limit);
            }

            // Generate Random Text
            propertyCharsA[i] = letters[(int) (Math.random() * letters.length)];
            propertyCharsB[i] = letters[(int) (Math.random() * letters.length)];

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
            propertyA= toSuitableString(propertyCharsA);
            a = new SingletonKey(propertyA, classNameA).hashCode();
            a = spread(a);
            a = 15 & a;
            propertyB = toSuitableString(propertyCharsB);
            b = new SingletonKey(propertyB, classNameB).hashCode();
            b = spread(b);
            b = 15 & b;
            i++;
        } while (a != b);
        
        LogManager.getLogger().info("Colliding Strings:");
        LogManager.getLogger().info(classNameA + " " + propertyA + " => " + a);
        LogManager.getLogger().info(classNameB + " " + propertyB + " => " + b);

        MCRConfiguration2.set(propertyA, classNameA);
        MCRConfiguration2.set(propertyB, classNameB);
        MCRConfiguration2.set(COLLISION_PROPERTY_NAME, propertyB);

        return Pair.of(propertyA, propertyB);

    }

    /**
     * Internal Method of {@link java.util.concurrent.ConcurrentHashMap}.
     *
     * @see {@link java.util.concurrent.ConcurrentHashMap#spread(int)}
     */
    private int spread(int h) {
        return (h ^ (h >>> 16)) & 0x7fffffff /* all usable bits */;
    }

    private static String toSuitableString(char[] value) {
        return String.valueOf(value).trim().replaceAll("\\s+", "");
    }

}
