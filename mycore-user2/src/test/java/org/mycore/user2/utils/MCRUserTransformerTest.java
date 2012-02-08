/*
 * $Id$
 * $Revision: 5697 $ $Date: 08.02.2012 $
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

package org.mycore.user2.utils;

import static org.junit.Assert.*;

import java.io.IOException;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.MCRHibTestCase;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.MCRUserManagerTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUserTransformerTest extends MCRHibTestCase {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        MCRUserManagerTest.addMapping();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRHibTestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRCategory groupsCategory = MCRCategoryDAOImplTest.loadClassificationResource("/mcr-groups.xml");
        MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();
        DAO.addCategory(null, groupsCategory);
    }

    /**
     * Test method for {@link org.mycore.user2.utils.MCRUserTransformer#buildMCRUser(org.jdom.Element)}.
     * @throws IOException 
     */
    @Test
    public final void testBuildMCRUser() throws IOException {
        Element input = MCRURIResolver.instance().resolve("resource:test-user.xml");
        MCRUser mcrUser = MCRUserTransformer.buildMCRUser(input);
        Element output = MCRUserTransformer.buildExportableXML(mcrUser);
        XMLOutputter xout=new XMLOutputter(Format.getPrettyFormat());
        xout.output(input, System.out);
        xout.output(output, System.out);
        assertTrue("Input element is not the same as outputElement", MCRXMLHelper.deepEqual(input, output));
    }

}
