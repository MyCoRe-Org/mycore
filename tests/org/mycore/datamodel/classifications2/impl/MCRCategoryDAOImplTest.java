/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import org.mycore.common.MCRHibTestCase;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRCategoryDAOImplTest extends MCRHibTestCase {

    public void testCalculateLeftRightAndLevel() {
        MCRCategoryImpl co1 = new MCRCategoryImpl();
        co1.setId(MCRCategoryID.rootID("co1"));
        assertEquals(2, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(0, co1.getLevel());
        MCRCategoryImpl co2 = new MCRCategoryImpl();
        co2.setId(new MCRCategoryID(co1.getId().getRootID(),"co2"));
        co1.getChildren().add(co2);
        assertEquals(4, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(1, co2.getLevel());
        MCRCategoryImpl co3 = new MCRCategoryImpl();
        co3.setId(new MCRCategoryID(co1.getId().getRootID(),"co3"));
        co1.getChildren().add(co3);
        assertEquals(6, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(1, co3.getLevel());
        MCRCategoryImpl co4 = new MCRCategoryImpl();
        co4.setId(new MCRCategoryID(co1.getId().getRootID(),"co4"));
        co3.getChildren().add(co4);
        assertEquals(8, MCRCategoryDAOImpl.calculateLeftRightAndLevel(co1, 1, 0));
        assertEquals(2, co4.getLevel());
    }

}
