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

package org.mycore.common.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNodeBuilderTest extends MCRTestCase {

    @Test
    public void testBuildingElements() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder().buildElement("element", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertTrue(built.getText().isEmpty());

        built = new MCRNodeBuilder().buildElement("element", "text", null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertEquals("text", built.getText());

        Element root = new Element("root");
        built = new MCRNodeBuilder().buildElement("element", null, root);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("root", built.getParentElement().getName());
    }

    @Test
    public void testBuildingAttributes() throws JaxenException, JDOMException {
        Attribute built = new MCRNodeBuilder().buildAttribute("@attribute", null, null);
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertTrue(built.getValue().isEmpty());

        built = new MCRNodeBuilder().buildAttribute("@attribute", "value", null);
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("value", built.getValue());

        Element parent = new Element("parent");
        built = new MCRNodeBuilder().buildAttribute("@attribute", null, parent);
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertNotNull(built.getParent());
        assertEquals("parent", built.getParent().getName());
    }

    @Test
    public void testBuildingValues() throws JaxenException, JDOMException {
        Attribute built = new MCRNodeBuilder().buildAttribute("@attribute='A \"test\"'", "ignore", null);
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("A \"test\"", built.getValue());

        built = new MCRNodeBuilder().buildAttribute("@attribute=\"O'Brian\"", null, null);
        assertNotNull(built);
        assertEquals("attribute", built.getName());
        assertEquals("O'Brian", built.getValue());

        built = new MCRNodeBuilder().buildAttribute("@mime=\"text/plain\"", null, null);
        assertNotNull(built);
        assertEquals("mime", built.getName());
        assertEquals("text/plain", built.getValue());

        Element element = new MCRNodeBuilder().buildElement("name=\"O'Brian\"", null, null);
        assertNotNull(element);
        assertEquals("name", element.getName());
        assertEquals("O'Brian", element.getText());
    }

    @Test
    public void testBuildingTrees() throws JaxenException, JDOMException {
        Element root = new Element("root");
        Attribute built = new MCRNodeBuilder().buildAttribute("parent/child/@attribute", null, root);
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
    public void testFirstNodeBuilt() throws JaxenException, JDOMException {
        MCRNodeBuilder builder = new MCRNodeBuilder();
        builder.buildElement("element", null, null);
        assertEquals("element", ((Element) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        builder.buildAttribute("@attribute", null, null);
        assertEquals("attribute", ((Attribute) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        builder.buildElement("element", "value", null);
        assertEquals("element", ((Element) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        builder.buildAttribute("@attribute", "value", null);
        assertEquals("attribute", ((Attribute) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        Element parent = builder.buildElement("root/parent", null, null);
        assertEquals("root", ((Element) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        builder.buildElement("parent/child/grandchild", null, parent.getParent());
        assertEquals("child", ((Element) (builder.getFirstNodeBuilt())).getName());

        builder = new MCRNodeBuilder();
        builder.buildElement("parent/child/grandchild", null, parent.getParent());
        assertNull(builder.getFirstNodeBuilt());

        builder = new MCRNodeBuilder();
        builder.buildElement("parent/child[2]/grandchild", null, parent.getParent());
        assertEquals("child", ((Element) (builder.getFirstNodeBuilt())).getName());
    }

    @Test
    public void testSimplePredicates() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder().buildElement("element[child]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));

        built = new MCRNodeBuilder().buildElement("element[child/grandchild]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));
        assertNotNull(built.getChild("child").getChild("grandchild"));

        built = new MCRNodeBuilder().buildElement("parent[child1]/child2", null, null);
        assertNotNull(built);
        assertEquals("child2", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("parent", built.getParentElement().getName());
        assertNotNull(built.getParentElement().getChild("child1"));
    }

    @Test
    public void testPredicatesWithValues() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder()
            .buildElement("contributor[role/roleTerm[@type='code'][@authority='ude']='author']", null, null);
        assertNotNull(built);
        assertEquals("contributor", built.getName());
        assertNotNull(built.getChild("role"));
        assertNotNull(built.getChild("role").getChild("roleTerm"));
        assertEquals("code", built.getChild("role").getChild("roleTerm").getAttributeValue("type"));
        assertEquals("ude", built.getChild("role").getChild("roleTerm").getAttributeValue("authority"));
    }

    @Test
    public void testMultiplePredicates() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder().buildElement("element[child1][child2]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child1"));
        assertNotNull(built.getChild("child2"));

    }

    @Test
    public void testNestedPredicates() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder().buildElement("element[child[grandchild1]/grandchild2]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());
        assertNotNull(built.getChild("child"));
        assertNotNull(built.getChild("child").getChild("grandchild1"));
        assertNotNull(built.getChild("child").getChild("grandchild2"));

    }

    @Test
    public void testExpressionsToIgnore() throws JaxenException, JDOMException {
        Element built = new MCRNodeBuilder().buildElement("element[2]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());

        built = new MCRNodeBuilder().buildElement("element[contains(.,'foo')]", null, null);
        assertNotNull(built);
        assertEquals("element", built.getName());

        built = new MCRNodeBuilder().buildElement("foo|bar", null, null);
        assertNull(built);

        Attribute attribute = new MCRNodeBuilder().buildAttribute("@lang[preceding::*/foo='bar']", "value", null);
        assertNotNull(attribute);
        assertEquals("lang", attribute.getName());
        assertEquals("value", attribute.getValue());

        built = new MCRNodeBuilder().buildElement("parent/child/following::node/foo='bar'", null, null);
        assertNotNull(built);
        assertEquals("child", built.getName());
        assertNotNull(built.getParentElement());
        assertEquals("parent", built.getParentElement().getName());
        assertEquals(0, built.getChildren().size());
        assertEquals("", built.getText());
    }

    @Test
    public void testAlreadyExisting() throws JaxenException, JDOMException {
        Element existingChild = new MCRNodeBuilder().buildElement("parent/child", null, null);
        Element existingParent = existingChild.getParentElement();
        assertEquals(existingChild, new MCRNodeBuilder().buildNode("child", null, existingParent));

        Attribute existingAttribute = new MCRNodeBuilder().buildAttribute("parent/@attribute", null, null);
        existingParent = existingAttribute.getParent();
        Attribute resolvedAttribute = new MCRNodeBuilder().buildAttribute("@attribute", null, existingParent);
        assertEquals(existingAttribute, resolvedAttribute);
        assertEquals(existingParent, resolvedAttribute.getParent());

        Element child = new MCRNodeBuilder().buildElement("root/parent/child", null, null);
        Element root = child.getParentElement().getParentElement();
        Attribute attribute = new MCRNodeBuilder().buildAttribute("parent/child/@attribute", null, root);
        assertEquals(child, attribute.getParent());
        Element foo = new MCRNodeBuilder().buildElement("parent[child]/foo", null, root);
        assertEquals(child, foo.getParentElement().getChild("child"));

        Element parentWithChildValueX = new MCRNodeBuilder().buildElement("parent[child='X']", null, root);
        assertNotSame(child.getParentElement(), parentWithChildValueX);
        Element resolved = new MCRNodeBuilder().buildElement("parent[child='X']", null, root);
        assertEquals(parentWithChildValueX, resolved);

        child = new MCRNodeBuilder().buildElement("parent/child='X'", null, root);
        assertNotNull(child);
        assertEquals(parentWithChildValueX.getChild("child"), child);
    }

    @Test
    public void testNamespaces() throws JaxenException, JDOMException {
        Element role = new MCRNodeBuilder().buildElement("mods:name[@xlink:href='id']/mods:role[@type='creator']", null,
            null);
        assertEquals("role", role.getName());
        assertEquals(MCRConstants.MODS_NAMESPACE, role.getNamespace());
        assertEquals("creator", role.getAttributeValue("type"));
        assertEquals("id", role.getParentElement().getAttribute("href", MCRConstants.XLINK_NAMESPACE).getValue());
    }

    @Test
    public void testAssigningValueToLastGenreatedNode() throws JaxenException, JDOMException {
        String value = "value";
        Element generated = new MCRNodeBuilder().buildElement("titleInfo/title", value, null);
        assertEquals(value, generated.getText());
        assertNotEquals(value, generated.getParentElement().getText());
    }

    @Test
    public void testBuildingNodeName() throws JaxenException, JDOMException {
        Element generated = new MCRNodeBuilder().buildElement("mycoreobject/metadata/def.modsContainer/modsContainer",
            null, null);
        assertEquals("modsContainer", generated.getName());
        assertEquals("def.modsContainer", generated.getParentElement().getName());
    }

    @Test
    public void testBuildingRootComponents() throws JaxenException, JDOMException {
        Element existingRoot = new Element("root");
        existingRoot.setAttribute("type", "existing");
        Document document = new Document(existingRoot);

        Element returned = new MCRNodeBuilder().buildElement("root", null, document);
        assertSame(existingRoot, returned);

        returned = new MCRNodeBuilder().buildElement("root[foo]", null, document);
        assertSame(existingRoot, returned);
        assertNotNull(returned.getChild("foo"));

        returned = new MCRNodeBuilder().buildElement("root[@foo='bar']", null, document);
        assertSame(existingRoot, returned);
        assertEquals("bar", returned.getAttributeValue("foo"));

        returned = new MCRNodeBuilder().buildElement("root/child", "bar", document);
        assertEquals("child", returned.getName());
        assertSame(existingRoot, returned.getParent());
        assertEquals("bar", returned.getText());
    }
}
