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

package org.mycore.frontend.xeditor;

import java.io.IOException;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSubmissionTest extends MCRTestCase {

    @Test
    public void testSubmitTextfields() throws JDOMException, SAXException, JaxenException, IOException {
         new MCRXEditorTestRunner("testSubmitTextfields");
    }

    @Test
    public void testSubmitSingleCheckbox() throws JDOMException, SAXException, JaxenException, IOException {
         new MCRXEditorTestRunner("testSubmitSingleCheckbox");
    }
    
    @Test
    public void testSubmitSelectSingleOption() throws JDOMException, SAXException, JaxenException, IOException {
         new MCRXEditorTestRunner("testSubmitSelectSingleOption");
    }
    
    @Test
    public void testSubmitSelectMultipleOptions() throws JDOMException, SAXException, JaxenException, IOException {
         new MCRXEditorTestRunner("testSubmitSelectMultipleOptions");
    }
}
