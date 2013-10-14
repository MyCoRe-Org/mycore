/*
 * $Revision: 28114 $ 
 * $Date: 2013-10-11 18:04:09 +0200 (Fr, 11 Okt 2013) $
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

package org.mycore.frontend.xeditor.validation;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRNodeBuilder;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorValidatorTest extends MCRTestCase {

    private MCREditorSession buildSession(String template) throws JaxenException, JDOMException {
        MCREditorSession session = new MCREditorSession();
        Document editedXML = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        session.setEditedXML(editedXML);
        return session;
    }

    private void addRule(MCREditorSession session, String xPath, String... attributes) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < attributes.length;)
            map.put(attributes[i++], attributes[i++]);
        session.getValidator().addRule(xPath, map);
    }

    @Test
    public void testNoValidationRules() throws JDOMException, JaxenException {
        MCREditorSession session = buildSession("document");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));
    }

    @Test
    public void testRequired() throws JDOMException, JaxenException {
        MCREditorSession session = buildSession("document[title]");
        addRule(session, "/document/title", "required", "true");

        assertFalse(session.getValidator().isValid());
        assertEquals("true", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        MCRBinding binding = new MCRBinding("/document/title", false, session.getRootBinding());
        session.getValidator().setValidationMarker(binding);
        assertEquals("has-error", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_MARKER));

        session = buildSession("document[title='foo']");
        addRule(session, "/document/title", "required", "true");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        binding = new MCRBinding("/document/title", false, session.getRootBinding());
        session.getValidator().setValidationMarker(binding);
        assertEquals("has-success", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_MARKER));

        session = buildSession("document[title][title[2]='foo']");
        addRule(session, "/document/title", "required", "true");

        assertTrue(session.getValidator().isValid());
        assertEquals("false", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_FAILED));

        binding = new MCRBinding("/document/title", false, session.getRootBinding());
        session.getValidator().setValidationMarker(binding);
        assertEquals("has-success", session.getVariables().get(MCRXEditorValidator.XED_VALIDATION_MARKER));
    }
}
