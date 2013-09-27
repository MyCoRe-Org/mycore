/*
 * $Revision: 26669 $ 
 * $Date: 2013-04-08 09:37:07 +0200 (Mo, 08 Apr 2013) $
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

import org.jaxen.JaxenException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRepeatBindingTest extends MCRTestCase {

    @Test
    public void testRepeatBindingWithComplexPredicate() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder().buildElement("conditions[condition/@type='bingo'][condition[2]/@type='bongo']", null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);
        MCRBinding conditions = new MCRBinding("conditions", root);

        MCRRepeatBinding repeat = new MCRRepeatBinding("condition[contains(' foo bar ',concat(' ',@type,' '))]", conditions, 3, 5);
        assertEquals(3, repeat.getBoundNodes().size());

        MCRBinding binding = repeat.bindRepeatPosition();
        assertEquals(1, binding.getBoundNodes().size());
        assertEquals("/conditions/condition[3]", binding.getAbsoluteXPath());
        ((Element) (binding.getBoundNode())).setAttribute("value", "a");

        binding = repeat.bindRepeatPosition();
        assertEquals(1, binding.getBoundNodes().size());
        assertEquals("/conditions/condition[4]", binding.getAbsoluteXPath());
        ((Element) (binding.getBoundNode())).setAttribute("value", "b");

        binding = repeat.bindRepeatPosition();
        assertEquals(1, binding.getBoundNodes().size());
        assertEquals("/conditions/condition[5]", binding.getAbsoluteXPath());
        ((Element) (binding.getBoundNode())).setAttribute("value", "c");

        repeat.removeBoundNode(0); 
        assertEquals(4, doc.getRootElement().getChildren().size());
        assertEquals(2, repeat.getBoundNodes().size());
        assertEquals("b", ((Element) (repeat.getBoundNodes().get(0))).getAttributeValue("value"));
        assertEquals("c", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("value"));

        repeat.cloneBoundElement(0);
        assertEquals(5, doc.getRootElement().getChildren().size());
        assertEquals(3, repeat.getBoundNodes().size());
        assertEquals("b", ((Element) (repeat.getBoundNodes().get(0))).getAttributeValue("value"));
        assertEquals("b", ((Element) (repeat.getBoundNodes().get(1))).getAttributeValue("value"));
        assertEquals("c", ((Element) (repeat.getBoundNodes().get(2))).getAttributeValue("value"));
    }

    @Test
    public void testSwapParameter() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder().buildElement("parent[name='aa'][name='ab'][name='bc'][name='ac']", null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("parent/name[contains(text(),'a')]", root, 0, 0);
        assertEquals(3, repeat.getBoundNodes().size());

        System.out.println(repeat.getSwapParameter(1, 2));
        System.out.println(repeat.getSwapParameter(2, 3));
        
        assertEquals("/parent|0|1", repeat.getSwapParameter(1, 2));
        assertEquals("/parent|1|3", repeat.getSwapParameter(2, 3));
    }

    @Test
    public void testSwap() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder().buildElement("parent[name='a'][note][foo][name='b'][note[2]]", null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);

        MCRRepeatBinding repeat = new MCRRepeatBinding("parent/name", root, 2, 0);
        assertEquals(2, repeat.getBoundNodes().size());

        assertEquals("a", doc.getRootElement().getChildren().get(0).getText());
        assertEquals("b", doc.getRootElement().getChildren().get(3).getText());
        
        assertEquals("a", ((Element) (repeat.getBoundNodes().get(0))).getText());
        assertEquals("b", ((Element) (repeat.getBoundNodes().get(1))).getText());

        String swapParameter = repeat.getSwapParameter(1,2);
        repeat.swap(swapParameter);
        
        assertEquals("b", doc.getRootElement().getChildren().get(0).getText());
        assertEquals("a", doc.getRootElement().getChildren().get(3).getText());
    }
}
