/*
 * 
 * $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
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

package org.mycore.backend.hibernate;

import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.common.MCRHibTestCase;

/**
 * @author Thomas Scheffler
 * 
 */
public class MCRHIBXMLStoreTest extends MCRHibTestCase {
    private static MCRXMLTABLE entry = getEntry();

    private static MCRHIBXMLStore XMLSTORE;

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if (XMLSTORE == null) {
            XMLSTORE = new MCRHIBXMLStore();
            setProperty("MCR.Metadata.Type." + entry.getType(), "true", false);
            XMLSTORE.init(entry.getType());
        }
        sessionFactory.getCurrentSession().save(entry);
        startNewTransaction();
    }

    private static MCRXMLTABLE getEntry() {
        MCRXMLTABLE entry = new MCRXMLTABLE();
        entry.setId("MyCoRe_test_1");
        entry.setVersion(1);
        entry.setType("test");
        entry.setXmlByteArray(new byte[0]);
        return entry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.mycore.backend.hibernate.MCRHIBXMLStore#exist(java.lang.String, int)} .
     */
    public void testExist() {
        assertTrue("Could not find " + entry.getId(), XMLSTORE.exist(entry.getId(), entry.getVersion()));
        String wrongID = entry.getId() + "1";
        assertFalse("There should be no " + wrongID, XMLSTORE.exist(wrongID, entry.getVersion()));
    }

}
