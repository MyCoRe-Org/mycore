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
package org.mycore.validation.pdfa;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.mycore.common.MCRException;
import org.mycore.datamodel.niofs.MCRPath;
import org.w3c.dom.Document;

/**
 * MCRPdfAValidatorResolver is a custom URIResolver implementation used for resolving XML document URIs during
 * transformations. It resolves a specific URI, gathers validation results using MCRPdfAFunctions, and returns the
 * resulting XML document as a Source object.
 *<p><br>
 * When an XML transformation is performed, and an external resource URI is encountered in the document, the
 * 'resolve' method of this class is called to handle the resolution of the URI. In this case, the URI format is assumed
 * to be 'mcrpdfa:{derivateId}', where 'derivateId' is the ID of the object for which validation results are required.
 *<p><br>
 * The 'resolve' method takes the 'href' and 'base' parameters, but only the 'href' is used. The 'href' is split to
 * extract the 'derivateId', and then the MCRPdfAFunctions class is used to generate the XML document containing the
 * validation results. The resulting XML document is returned as a DOMSource object, which can be used in the XML
 * transformation process.
 *<p><br>
 * Note: This implementation assumes that the 'href' parameter follows the 'mcrpdfa:{derivateId}' format, and the 'base'
 * parameter is not used in the resolution process.
 */
public class MCRPDFAValidatorResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] hrefParts = href.split(":");
        String derivateId = hrefParts[1];
        MCRPath rootPath = MCRPath.getRootPath(derivateId);
        try {
            Document document = MCRPDFAFunctions.getResults(rootPath, derivateId);
            return new DOMSource(document);
        } catch (ParserConfigurationException | IOException e) {
            throw new MCRException(e);
        }
    }
}
