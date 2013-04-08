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

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRConstants;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNodeBuilderTest {

    @Test
    public void testBuildingElements() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("element", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertTrue(built.getText().isEmpty());

        built = (Element) (MCRNodeBuilder.build("element", "text", null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertEquals("text", built.getText());

        Element root = new Element("root");
        built = (Element) (MCRNodeBuilder.build("element", null, null, root));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("root", built.getParentElement().getName());
    }

    @Test
    public void testBuildingAttributes() throws ParseException, JDOMException {
        Attribute built = (Attribute) (MCRNodeBuilder.build("@attribute", null, null, null));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertTrue(built.getValue().isEmpty());

        built = (Attribute) (MCRNodeBuilder.build("@attribute", "value", null, null));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("value", built.getValue());

        Element parent = new Element("parent");
        built = (Attribute) (MCRNodeBuilder.build("@attribute", null, null, parent));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertNotNull(built.getParent());
        assertEquals("parent", built.getParent().getName());
    }

    @Test
    public void testBuildingValues() throws ParseException, JDOMException {
        Attribute built = (Attribute) (MCRNodeBuilder.build("@attribute='A \"test\"'", "ignore", null, null));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("A \"test\"", built.getValue());

        built = (Attribute) (MCRNodeBuilder.build("@attribute=\"O'Brian\"", null, null, null));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("O'Brian", built.getValue());

        built = (Attribute) (MCRNodeBuilder.build("@mime=\"text/plain\"", null, null, null));
        assertNotNull(built);
        assertEquals("mime", built.getName());
        assertEquals("text/plain", built.getValue());

        Element element = (Element) (MCRNodeBuilder.build("name=\"O'Brian\"", null, null, null));
        assertNotNull(element);
        assertEquals("name", element.getName());
        assertEquals("O'Brian", element.getText());
    }

    @Test
    public void testBuildingTrees() throws ParseException, JDOMException {
        Element root = new Element("root");
        Attribute built = (Attribute) (MCRNodeBuilder.build("parent/child/@attribute", null, null, root));
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertNotNull(built.getParent());
        assertEquals("child", built.getParent().getName());
        assertNotNull(built.getParent().getParentElement());
        assertEquals("parent", built.getParent().getParentElement().getName());
        assertNotNull(built.getParent().getParentElement().getParentElement());
        assertEquals("root", built.getParent().getParentElement().getParentElement().getName());
    }

    @Test
    public void testSimplePredicates() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("element[child]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));

        built = (Element) (MCRNodeBuilder.build("element[child/grandchild]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));
        assertNotNull(built.getChild("child").getChild("grandchild"));

        built = (Element) (MCRNodeBuilder.build("parent[child1]/child2", null, null, null));
        assertNotNull(built);
        assertEquals("child2", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("parent", built.getParentElement().getName());
        assertNotNull(built.getParentElement().getChild("child1"));
    }

    @Test
    public void testPredicatesWithValues() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("contributor[role/roleTerm[@type='code'][@authority='ude']='author']", null, null, null));
        assertNotNull(built);
        assertEquals("contributor", built.getName());
        assertNotNull(built.getChild("role"));
        assertNotNull(built.getChild("role").getChild("roleTerm"));
        assertEquals("code", built.getChild("role").getChild("roleTerm").getAttributeValue("type"));
        assertEquals("ude", built.getChild("role").getChild("roleTerm").getAttributeValue("authority"));
    }

    @Test
    public void testMultiplePredicates() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("element[child1][child2]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child1"));
        assertNotNull(built.getChild("child2"));

    }

    @Test
    public void testNestedPredicates() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("element[child[grandchild1]/grandchild2]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));
        assertNotNull(built.getChild("child").getChild("grandchild1"));
        assertNotNull(built.getChild("child").getChild("grandchild2"));

    }

    @Test
    public void testExpressionsToIgnore() throws ParseException, JDOMException {
        Element built = (Element) (MCRNodeBuilder.build("element[2]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());

        built = (Element) (MCRNodeBuilder.build("element[contains(.,'foo')]", null, null, null));
        assertNotNull(built);
        assertEquals("element", built.getName());

        built = (Element) (MCRNodeBuilder.build("foo|bar", null, null, null));
        assertNull(built);

        Attribute attribute = (Attribute) (MCRNodeBuilder.build("@lang[preceding::*/foo='bar']", "value", null, null));
        assertNotNull(attribute);
        assertEquals("lang", attribute.getName());
        assertEquals("value", attribute.getValue());

        built = (Element) (MCRNodeBuilder.build("parent/child/following::node/foo='bar'", null, null, null));
        assertNotNull(built);
        assertEquals("child", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("parent", built.getParentElement().getName());
        assertEquals(0, built.getChildren().size());
        assertEquals("", built.getText());
    }

    @Test
    public void testAlreadyExisting() throws ParseException, JDOMException {
        Element existingChild = (Element) (MCRNodeBuilder.build("parent/child", null, null, null));
        Element existingParent = existingChild.getParentElement();
        assertEquals(existingChild, MCRNodeBuilder.build("child", null, null, existingParent));

        Attribute existingAttribute = (Attribute) (MCRNodeBuilder.build("parent/@attribute", null, null, null));
        existingParent = existingAttribute.getParent();
        Attribute resolvedAttribute = (Attribute) (MCRNodeBuilder.build("@attribute", null, null, existingParent));
        assertEquals(existingAttribute, resolvedAttribute);
        assertEquals(existingParent, resolvedAttribute.getParent());

        Element child = (Element) (MCRNodeBuilder.build("root/parent/child", null, null, null));
        Element root = child.getParentElement().getParentElement();
        Attribute attribute = (Attribute) (MCRNodeBuilder.build("parent/child/@attribute", null, null, root));
        assertEquals(child, attribute.getParent());
        Element foo = (Element) (MCRNodeBuilder.build("parent[child]/foo", null, null, root));
        assertEquals(child, foo.getParentElement().getChild("child"));

        Element parentWithChildValueX = (Element) (MCRNodeBuilder.build("parent[child='X']", null, null, root));
        assertNotSame(child.getParentElement(), parentWithChildValueX);
        Element resolved = (Element) (MCRNodeBuilder.build("parent[child='X']", null, null, root));
        assertEquals(parentWithChildValueX, resolved);

        child = (Element) (MCRNodeBuilder.build("parent/child='X'", null, null, root));
        assertNotNull(child);
        assertEquals(parentWithChildValueX.getChild("child"), child);
    }

    @Test
    public void testNamespaces() throws ParseException, JDOMException {
        Element role = (Element) (MCRNodeBuilder.build("mods:name[@xlink:href='id']/mods:role[@type='creator']", null, null, null));
        assertEquals("role", role.getName());
        assertEquals(MCRConstants.MODS_NAMESPACE, role.getNamespace());
        assertEquals("creator", role.getAttributeValue("type"));
        assertEquals("id", role.getParentElement().getAttribute("href", MCRConstants.XLINK_NAMESPACE).getValue());
    }
}
