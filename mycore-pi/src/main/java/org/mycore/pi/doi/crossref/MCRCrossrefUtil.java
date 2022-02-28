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

package org.mycore.pi.doi.crossref;

import java.util.List;
import java.util.Objects;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.function.MCRThrowFunction;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import jakarta.validation.constraints.NotNull;

/**
 * Proivides Util functions for Crossref registration.
 */
public class MCRCrossrefUtil {

    private static final Namespace CR = MCRConstants.CROSSREF_NAMESPACE;

    /**
     * Inserts informations to the crossref head element.
     * @param headElement existing head element
     * @param batchID Publisher generated ID that uniquely identifies the DOI submission batch. It will be used as a
     *                reference in error messages sent by the MDDB, and can be used for submission tracking. The
     *                publisher must insure that this number is unique for every submission to CrossRef.
     * @param timestamp Indicates version of a batch file instance or DOI. timestamp is used
     * 				to uniquely identify batch files and DOI values when a DOI has been updated one or
     * 				more times. timestamp is an integer representation of date and time that serves as a
     * 				version number for the record that is being deposited. Because CrossRef uses it as a
     * 				version number, the format need not follow any public standard and therefore the
     * 				publisher can determine the internal format. The schema format is a double of at
     * 				least 64 bits, insuring that a fully qualified date/time stamp of 19 digits can be
     * 				submitted. When depositing data, CrossRef will check to see if a DOI has already
     * 				been deposited for the specific doi value. If the newer data carries a time stamp
     * 				value that is equal to or greater than the old data based on a strict numeric
     * 				comparison, the new data will replace the old data. If the new data value is less
     * 				than the old data value, the new data will not replace the old data. timestamp is
     * 				optional in doi_data and required in head. The value from the head instance
     * 				timestamp will be used for all instances of doi_data that do not include a timestamp
     * 				element.
     * @param depositorName Name of the organization registering the DOIs. The name placed in this element should match
     *                      the name under which a depositing organization has registered with CrossRef.
     * @param depositorMail e-mail address to which batch success and/or error messages are sent.
     *                      It is recommended that this address be unique to a position within the organization
     *                      submitting data (e.g. "doi@...") rather than unique to a person. In this way, the
     *                      alias for delivery of this mail can be changed as responsibility for submission of
     *                      DOI data within the organization changes from one person to another.
     * @param registrant The organization that owns the information being registered.
     */
    public static void insertBatchInformation(@NotNull Element headElement, @NotNull String batchID,
        @NotNull String timestamp, @NotNull String depositorName, @NotNull String depositorMail,
        @NotNull String registrant) {
        headElement.getChild("doi_batch_id", CR).setText(Objects.requireNonNull(batchID));
        headElement.getChild("timestamp", CR).setText(Objects.requireNonNull(timestamp));
        final Element depositorElement = headElement.getChild("depositor", CR);
        depositorElement.getChild("depositor_name", CR).setText(Objects.requireNonNull(depositorName));
        depositorElement.getChild("email_address", CR).setText(Objects.requireNonNull(depositorMail));
        headElement.getChild("registrant", CR).setText(Objects.requireNonNull(registrant));
    }

    public static void replaceDOIData(Element root,
        MCRThrowFunction<String, String, MCRPersistentIdentifierException> idDOIFunction, String baseURL)
        throws MCRPersistentIdentifierException {
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> xp = xpfac
            .compile(".//cr:doi_data_replace", Filters.element(), null, MCRConstants.getStandardNamespaces());
        List<Element> evaluate = xp.evaluate(root);
        for (Element element : evaluate) {
            final String mycoreObjectID = element.getText();
            final String doi = idDOIFunction.apply(mycoreObjectID);
            element.removeContent();
            if (doi != null) {
                element.addContent(new Element("doi", CR).setText(doi));
                element.addContent(new Element("resource", CR).setText(baseURL + "/receive/" + mycoreObjectID));
                element.setName("doi_data");
            } else {
                element.getParentElement().removeContent(element);
            }
        }

    }

}
