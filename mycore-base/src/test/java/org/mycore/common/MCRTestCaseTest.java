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

package org.mycore.common;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration2;

@MCRTestConfiguration(properties = {
    //overwrite property of MCRTestCase
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "false"),
    //overwrite property of config/mycore.properties
    @MCRTestProperty(key = "MCR.NameOfProject", string = MCRTestCaseTest.PROJECT_NAME)
})
public final class MCRTestCaseTest extends MCRTestCase {

    final static String PROJECT_NAME = "MyCoRe Test";

    @Test
    public void testConfigAnnotationOverwrite() {
        Assert.assertFalse(MCRConfiguration2.getBoolean("MCR.Metadata.Type.test").get());
    }

    @Test
    public void testConfigPropertiesOverwrite() {
        Assert.assertEquals(PROJECT_NAME, MCRConfiguration2.getStringOrThrow("MCR.NameOfProject"));
    }

    @Test
    public void testConfigProperties() {
        Assert.assertNotNull(MCRConfiguration2.getStringOrThrow("MCR.CommandLineInterface.SystemName"));
    }
}
