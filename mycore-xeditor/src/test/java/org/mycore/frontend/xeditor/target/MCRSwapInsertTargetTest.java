/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor.target;

import static org.junit.Assert.assertEquals;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRRepeatBinding;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSwapInsertTargetTest extends MCRTestCase {

    @Test
    public void testSwapParameter() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder().buildElement("parent[name='aa'][name='ab'][name='bc'][name='ac']", null,
            null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("parent/name[contains(text(),'a')]", root, 0, 0, "build");
        assertEquals(3, repeat.getBoundNodes().size());

        repeat.bindRepeatPosition();
        repeat.bindRepeatPosition();
        assertEquals("/parent|1|build|name[contains(text(), \"a\")]",
            MCRSwapTarget.getSwapParameter(repeat, MCRSwapTarget.MOVE_UP));
        assertEquals("/parent|2|build|name[contains(text(), \"a\")]",
            MCRSwapTarget.getSwapParameter(repeat, MCRSwapTarget.MOVE_DOWN));
    }

    @Test
    public void testSwap() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder().buildElement("parent[name='a'][note][foo][name='b'][note[2]]", null,
            null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("parent/name", root, 2, 0, "build");
        assertEquals(2, repeat.getBoundNodes().size());

        assertEquals("a", doc.getRootElement().getChildren().get(0).getText());
        assertEquals("b", doc.getRootElement().getChildren().get(3).getText());

        assertEquals("a", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("b", ((Element) (repeat.getBoundNodes().get(1))).getText());

        repeat.bindRepeatPosition();
        String swapParam = MCRSwapTarget.getSwapParameter(repeat, MCRSwapTarget.MOVE_DOWN);
        new MCRSwapTarget().handle(swapParam, root);

        assertEquals("b", doc.getRootElement().getChildren().get(0).getText());
        assertEquals("a", doc.getRootElement().getChildren().get(3).getText());
    }

    @Test
    public void testBuildInsert() throws JaxenException, JDOMException {
        String x = "mods:mods[mods:name[@type='personal']='p1'][mods:name[@type='personal'][2]='p2'][mods:name[@type='corporate']='c1']";
        Element template = new MCRNodeBuilder().buildElement(x, null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 3, 10, "build");
        assertEquals("mods:name[(@type = \"personal\")]", repeat.getElementNameWithPredicates());

        assertEquals(3, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("", ((Element) (repeat.getBoundNodes().get(2))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(2))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(2))).getAttributeValue("type"));

        repeat.insert(1);
        assertEquals(4, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(1))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("type"));
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(2))).getText());

        repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='corporate']", root, 1, 10, "build");
        assertEquals(1, repeat.getBoundNodes().size());
        assertEquals("mods:name[(@type = \"corporate\")]", repeat.getElementNameWithPredicates());
    }

    @Test
    public void testCloneInsert() throws JaxenException, JDOMException {
        String x = "mods:mods[mods:name[@type='personal']='p1'][mods:name[@type='personal'][2]='p2'][mods:name[@type='corporate']='c1']";
        Element template = new MCRNodeBuilder().buildElement(x, null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 3, 10, "clone");
        assertEquals("mods:name[(@type = \"personal\")]", repeat.getElementNameWithPredicates());

        assertEquals(3, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(2))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(2))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(2))).getAttributeValue("type"));

        repeat.insert(1);
        assertEquals(4, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(1))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("type"));
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(2))).getText());

        repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='corporate']", root, 1, 10, "build");
        assertEquals(1, repeat.getBoundNodes().size());
        assertEquals("mods:name[(@type = \"corporate\")]", repeat.getElementNameWithPredicates());
    }

    @Test
    public void testBuildInsertParam() throws JaxenException, JDOMException {
        String x = "mods:mods[mods:name[@type='personal']='p1'][mods:name[@type='personal'][2]='p2'][mods:name[@type='corporate']='c1']";
        Element template = new MCRNodeBuilder().buildElement(x, null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 2, 10, "build");
        repeat.bindRepeatPosition();
        String insertParam = MCRInsertTarget.getInsertParameter(repeat);
        assertEquals("/mods:mods|1|build|mods:name[(@type = \"personal\")]", insertParam);
        repeat.detach();

        new MCRInsertTarget().handle(insertParam, root);
        repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 1, 10, "build");
        assertEquals(3, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(1))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("type"));
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(2))).getText());
    }

    @Test
    public void testCloneInsertParam() throws JaxenException, JDOMException {
        String x = "mods:mods[mods:name[@type='personal']='p1'][mods:name[@type='personal'][2]='p2'][mods:name[@type='corporate']='c1']";
        Element template = new MCRNodeBuilder().buildElement(x, null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 2, 10, "clone");
        repeat.bindRepeatPosition();
        String insertParam = MCRInsertTarget.getInsertParameter(repeat);
        assertEquals("/mods:mods|1|clone|mods:name[(@type = \"personal\")]", insertParam);
        repeat.detach();

        new MCRInsertTarget().handle(insertParam, root);
        repeat = new MCRRepeatBinding("mods:mods/mods:name[@type='personal']", root, 1, 10, "build");
        assertEquals(3, repeat.getBoundNodes().size());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("p1", ((Element) (repeat.getBoundNodes().get(1))).getText());
        assertEquals("name", ((Element) (repeat.getBoundNodes().get(1))).getName());
        assertEquals("personal", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("type"));
        assertEquals("p2", ((Element) (repeat.getBoundNodes().get(2))).getText());
    }
}
