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

package org.mycore.pi.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRPIAndPredicateTest {

    private static final String KEY_CREATION_PREDICATE = "MCR.PI.Service.Mock.CreationPredicate";

    private static final String KEY_CREATION_PREDICATE_1 = KEY_CREATION_PREDICATE + ".1";

    private static final String KEY_CREATION_PREDICATE_1_1 = KEY_CREATION_PREDICATE + ".1.1";

    private static final String KEY_CREATION_PREDICATE_1_2 = KEY_CREATION_PREDICATE + ".1.2";

    private static final String KEY_CREATION_PREDICATE_2 = KEY_CREATION_PREDICATE + ".2";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1, classNameOf = MCRTruePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_2, classNameOf = MCRTruePredicate.class)
    })
    void testTrueAndTrue() {
        assertTrue(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1, classNameOf = MCRTruePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_2, classNameOf = MCRFalsePredicate.class)
    })
    void testTrueAndFalse() {
        assertFalse(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1_1, classNameOf = MCRTruePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1_2, classNameOf = MCRTruePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_2, classNameOf = MCRTruePredicate.class)
    })
    void testNestedTrueAndTrue() {
        assertTrue(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1, classNameOf = MCRPIAndPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1_1, classNameOf = MCRTruePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_1_2, classNameOf = MCRFalsePredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_2, classNameOf = MCRFalsePredicate.class)
    })
    void testNestedTrueAndFalse() {
        assertFalse(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    private static MCRObject getTestObject() {

        MCRObject mcrObject = new MCRObject();
        mcrObject.setSchema("test");
        mcrObject.setId(MCRObjectID.getInstance("mcr_test_00000001"));

        return mcrObject;

    }

}
