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
 * {@link URIResolver} that validates PDF/A files in a given derivate and returns the results as XML.
 */
public class MCRPDFAValidatorResolver implements URIResolver {

    /**
     * Validates all PDF/A files in the given derivate and returns the validation results as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{derivateId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   pdfAValidator:mcr_derivate_00000001
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <pdfa>
     *     <result file="/document.pdf" valid="true"/>
     *     <result file="/other.pdf" valid="false"/>
     *   </pdfa>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link DOMSource} wrapping the validation results document
     * @throws TransformerException if the validation results cannot be generated
     */
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
