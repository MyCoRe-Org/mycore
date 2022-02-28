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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;
import org.xml.sax.SAXException;

import jakarta.servlet.ServletContext;

/**
 * Builds an XSL template that can be used to filter the XML content to
 * the schema that can be edited by this form.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBuildFilterXSLTarget implements MCREditorTarget {

    private static final String TRANSFORMER_ID = "BuildFilterXSL";

    private MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer(TRANSFORMER_ID);

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session,
        String servletNameOrPath) throws Exception {
        Document result = session.getEditedXML();
        result = MCRChangeTracker.removeChangeTracking(result);

        MCRContent xsl = transformer.transform(new MCRJDOMContent(result));
        xsl = removeDummyAttributes(xsl);

        MCRLayoutService.instance().sendXML(job.getRequest(), job.getResponse(), xsl);
    }

    /**
     * There are dummy attributes in the root element of the XSL, which are removed here.
     * Those attributes were generated during transformation to force passing through namespace nodes.
     */
    private MCRContent removeDummyAttributes(MCRContent xsl) throws JDOMException, IOException, SAXException {
        Document doc = xsl.asXML();
        List<Attribute> attributes = doc.getRootElement().getAttributes();

        for (Iterator<Attribute> iter = attributes.iterator(); iter.hasNext();) {
            if ("dummy".equals(iter.next().getName())) {
                iter.remove();
            }
        }
        MCRJDOMContent out = new MCRJDOMContent(doc);
        out.setFormat(Format.getPrettyFormat().setIndent("  "));
        return out;
    }
}
