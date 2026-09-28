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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Resolves {@code pdfAReport:{derivateId}} to the persisted PDF/A validation reports of a derivate.
 * <p>
 * Unlike {@link MCRPDFAValidatorResolver} this resolver never validates a PDF file itself. Files without an up to
 * date report are reported as {@code <file name="..." status="pending"/>} and their validation is scheduled, so
 * that a later request can present the result.
 *
 * @see MCRPDFAReportManager#getReportSummary(MCRObjectID)
 */
public class MCRPDFAReportResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        MCRObjectID derivateId = MCRObjectID.getInstance(href.substring(href.indexOf(':') + 1));
        try {
            return new DOMSource(MCRPDFAReportManager.getReportSummary(derivateId));
        } catch (ParserConfigurationException e) {
            throw new TransformerException(e);
        }
    }
}
