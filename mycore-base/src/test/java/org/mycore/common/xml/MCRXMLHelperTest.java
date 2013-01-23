/*
 * $Id$
 * $Revision: 5697 $ $Date: 21.09.2010 $
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

package org.mycore.common.xml;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRXMLHelperTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.common.xml.MCRXMLHelper#deepEqual(org.jdom2.Element, org.jdom2.Element)}.
     */
    @Test
    public void testDeepEqualElementElement() {
        assertTrue("Elements should be equal", MCRXMLHelper.deepEqual(getSmallElement(), getSmallElement()));
        assertTrue("Elements should be equal", MCRXMLHelper.deepEqual(getBigElement(), getBigElement()));
        assertFalse("Elements should be different", MCRXMLHelper.deepEqual(getSmallElement(), getBigElement()));
        assertFalse("Elements should be different", MCRXMLHelper.deepEqual(getBigElement(), getSmallElement()));
    }

    private static Element getSmallElement() {
        Element elm = new Element("test");
        elm.setAttribute("j", "unit");
        elm.setAttribute("junit", "test");
        elm.addContent(new Element("junit"));
        return elm;
    }
    
    private static Element getBigElement(){
        Element elm=getSmallElement();
        elm.addContent(new Element("junit"));
        return elm;
    }

}
