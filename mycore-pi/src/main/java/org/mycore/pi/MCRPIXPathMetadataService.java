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

package org.mycore.pi;

import java.util.List;
import java.util.Optional;

import javax.naming.OperationNotSupportedException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPIXPathMetadataService extends MCRPIMetadataService<MCRPersistentIdentifier> {

    public MCRPIXPathMetadataService(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRPersistentIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        String xpath = getProperties().get("Xpath");
        Document xml = obj.createXML();
        MCRNodeBuilder nb = new MCRNodeBuilder();
        try {
            nb.buildElement(xpath, identifier.asString(), xml);
            if (obj instanceof MCRObject) {
                final Element metadata = xml.getRootElement().getChild("metadata");
                ((MCRObject) obj).getMetadata().setFromDOM(metadata);
            } else {
                throw new MCRPersistentIdentifierException(obj.getId() + " is no MCRObject!",
                    new OperationNotSupportedException(getClass().getName() + " only supports "
                        + MCRObject.class.getName() + "!"));
            }

        } catch (Exception e) {
            throw new MCRException("Error while inscribing PI to " + obj.getId(), e);

        }
    }

    @Override
    public void removeIdentifier(MCRPersistentIdentifier identifier, MCRBase obj, String additional) {
        String xpath = getProperties().get("Xpath");
        Document xml = obj.createXML();
        XPathFactory xPathFactory = XPathFactory.instance();
        XPathExpression<Element> xp = xPathFactory.compile(xpath, Filters.element());
        List<Element> elements = xp.evaluate(xml);

        elements.stream()
            .filter(element -> element.getTextTrim().equals(identifier.asString()))
            .forEach(Element::detach);
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        String xpath = getProperties().get("Xpath");
        Document xml = obj.createXML();
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> xp = xpfac
            .compile(xpath, Filters.element(), null, MCRConstants.getStandardNamespaces());
        List<Element> evaluate = xp.evaluate(xml);
        if (evaluate.size() > 1) {
            throw new MCRPersistentIdentifierException(
                "Got " + evaluate.size() + " matches for " + obj.getId() + " with xpath " + xpath + "");
        }

        if (evaluate.size() == 0) {
            return Optional.empty();
        }

        Element identifierElement = evaluate.listIterator().next();
        String identifierString = identifierElement.getTextNormalize();

        Optional<MCRPersistentIdentifier> parsedIdentifierOptional = MCRPIManager.getInstance()
            .getParserForType(getProperties().get("Type")).parse(identifierString);
        return parsedIdentifierOptional.map(MCRPersistentIdentifier.class::cast);
    }

}
