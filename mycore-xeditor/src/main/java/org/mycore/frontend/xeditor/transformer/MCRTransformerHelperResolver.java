/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor.transformer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * Implements URIResolver to assist transforming xed to html.
 * This resolver is be called via xsl document() function from within xeditor.xsl.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRTransformerHelperResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        MCRTransformerHelperCall call = new MCRTransformerHelperCall(href);

        try {
            handleCall(call);
        } catch (TransformerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }

        Element returnElement = call.getReturnElement();
        if (returnElement.hasAttributes() || (returnElement.getContentSize() > 0)) {
            Source source = new JDOMSource(returnElement);
            source.setSystemId(source.getSystemId() + Math.random());
            return source;
        } else {
            return null;
        }
    }

    private void handleCall(MCRTransformerHelperCall call) throws Exception {
        MCRTransformationState tfhelper = call.getTransformerHelper();
        tfhelper.getMethodHelperMap().get(call.getMethod()).handle(call);
    }
}
