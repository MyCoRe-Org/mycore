/*
 * $Id$
 * $Revision: 5697 $ $Date: 30.09.2010 $
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

package org.mycore.datamodel.metadata;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRObjectIDPoolTest extends MCRTestCase {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void getInstance() {
        runGarbageCollection();
        long before = MCRObjectIDPool.getSize();
        String id = "MyCoRe_test_11111111";
        @SuppressWarnings("unused")
        MCRObjectID mcrId = MCRObjectIDPool.getMCRObjectID(id);
        assertEquals("ObjectIDPool size is different", before + 1, MCRObjectIDPool.getSize());
        mcrId = null;
        runGarbageCollection();
        assertNull("ObjectIDPool should not contain ID anymore.", MCRObjectIDPool.getIfPresent(id));
        assertEquals("ObjectIDPool size is different", before, MCRObjectIDPool.getSize());
    }

    private void runGarbageCollection() {
        final int garbageIterations = 2;
        for (int i = 0; i < garbageIterations; i++) {
            System.gc();
            System.runFinalization();
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }
}
