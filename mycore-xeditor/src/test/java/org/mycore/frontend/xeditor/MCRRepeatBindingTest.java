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

import static org.junit.Assert.assertEquals;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRepeatBindingTest extends MCRTestCase {

    @Test
    public void testRepeatBindingWithComplexPredicate() throws JaxenException, JDOMException {
        Element template = new MCRNodeBuilder()
            .buildElement("conditions[condition/@type='bingo'][condition[2]/@type='bongo']", null, null);
        Document doc = new Document(template);
        MCRBinding root = new MCRBinding(doc);
        MCRBinding conditions = new MCRBinding("conditions", true, root);

        MCRRepeatBinding repeat = new MCRRepeatBinding("condition[contains(' foo bar ',concat(' ',@type,' '))]",
            conditions, 3, 5, "build");
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
}
