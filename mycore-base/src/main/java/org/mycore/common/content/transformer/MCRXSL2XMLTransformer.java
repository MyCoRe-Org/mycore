/*
 * $Id$
 * $Revision: 5697 $ $Date: 01.03.2012 $
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

package org.mycore.common.content.transformer;

import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.xml.sax.SAXException;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet
 * 
 * Resulting MCRContent holds XML.
 * Use {@link MCRXSLTransformer} if you want to produce non XML output.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRXSL2XMLTransformer extends MCRXSLTransformer {

    @Override
    protected MCRContent transform(Transformer transformer, MCRContent source) throws TransformerException, IOException {
        JDOMResult result = new JDOMResult();
        transformer.transform(source.getSource(), result);
        Document resultDoc = result.getDocument();
        if (resultDoc == null) {
            try {
                throw new TransformerException("Stylesheet " + templates.getSource().getSystemId() + " does not return any content for " + source.getSystemId());
            } catch (SAXException e) {
                throw new TransformerException("Stylesheet " + templates.toString() + " does not return any content for " + source.getSystemId());
            }
        }
        return new MCRJDOMContent(result.getDocument());
    }

    @Override
    protected String getDefaultExtension() {
        return "xml";
    }

}
