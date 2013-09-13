/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.junit.Test;
import org.mycore.frontend.xeditor.MCRXPathParser.MCRXPath;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathParserTest {

    @Test
    public void testParser() throws ParseException {
        MCRXPath xPath = MCRXPathParser.parse("element");
        assertNotNull(xPath);
        assertNotNull(xPath.getLocationSteps());
        assertNotNull(xPath.getLocationSteps().get(0));
        assertEquals("element", xPath.getLocationSteps().get(0).getQualifiedName());
        assertNull(xPath.getLocationSteps().get(0).getAssignedValue());
        assertEquals(0, xPath.getLocationSteps().get(0).getPredicates().size());
        assertEquals("element", xPath.toString());

        xPath = MCRXPathParser.parse("parent/child/subchild");
        assertEquals("parent", xPath.getLocationSteps().get(0).getQualifiedName());
        assertEquals("child", xPath.getLocationSteps().get(1).getQualifiedName());
        assertEquals("subchild", xPath.getLocationSteps().get(2).getQualifiedName());
        assertEquals("parent/child/subchild", xPath.toString());

        xPath = MCRXPathParser.parse("@value=\"O'Brian [1918-1986]\"");
        assertEquals("value", xPath.getLocationSteps().get(0).getQualifiedName());
        assertTrue(xPath.getLocationSteps().get(0).isAttribute());
        assertEquals("O'Brian [1918-1986]", xPath.getLocationSteps().get(0).getAssignedValue().getValue());
        assertEquals("@value=\"O'Brian [1918-1986]\"", xPath.toString());

        xPath = MCRXPathParser.parse("contributor[role/roleTerm[type='code'][encoding='text/plain']]/name");
        assertEquals(2, xPath.getLocationSteps().size());
        assertEquals("contributor", xPath.getLocationSteps().get(0).getQualifiedName());
        assertEquals("name", xPath.getLocationSteps().get(1).getQualifiedName());
        assertEquals(1, xPath.getLocationSteps().get(0).getPredicates().size());
        assertEquals("role", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(0).getQualifiedName());
        assertEquals("roleTerm", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getQualifiedName());
        assertEquals(2, xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getPredicates().size());
        assertEquals("type", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getPredicates().get(0)
                .getLocationSteps().get(0).getQualifiedName());
        assertEquals("code", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getPredicates().get(0)
                .getLocationSteps().get(0).getAssignedValue().getValue());
        assertEquals("encoding", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getPredicates().get(1)
                .getLocationSteps().get(0).getQualifiedName());
        assertEquals("text/plain", xPath.getLocationSteps().get(0).getPredicates().get(0).getLocationSteps().get(1).getPredicates().get(1)
                .getLocationSteps().get(0).getAssignedValue().getValue());
        assertEquals("contributor[role/roleTerm[type='code'][encoding='text/plain']]/name", xPath.toString());
    }

    @Test
    public void testNamespaces() throws ParseException {
        MCRXPath xPath = MCRXPathParser.parse("mods:name/mods:namePart[@type='corporate'][@xlink:href]");
        assertEquals(2, xPath.getLocationSteps().size());
        assertEquals("mods:name", xPath.getLocationSteps().get(0).getQualifiedName());
        assertEquals("mods", xPath.getLocationSteps().get(0).getNamespacePrefix());
        assertEquals("name", xPath.getLocationSteps().get(0).getLocalName());
        assertEquals("mods:namePart", xPath.getLocationSteps().get(1).getQualifiedName());
        assertEquals("type", xPath.getLocationSteps().get(1).getPredicates().get(0).getLocationSteps().get(0).getQualifiedName());
        assertTrue(xPath.getLocationSteps().get(1).getPredicates().get(0).getLocationSteps().get(0).isAttribute());
        assertEquals("xlink:href", xPath.getLocationSteps().get(1).getPredicates().get(1).getLocationSteps().get(0).getQualifiedName());
        assertTrue(xPath.getLocationSteps().get(1).getPredicates().get(1).getLocationSteps().get(0).isAttribute());
    }
    
    @Test
    public void testComplexPredicate() throws ParseException {
        MCRXPath xPath = MCRXPathParser.parse("condition[contains(',mods.title,mods.author,mods.name,allMeta,', concat(',',@field,','))]");
    }
}
