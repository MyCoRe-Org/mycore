package org.mycore.pi.urn;

import java.io.IOException;
import java.util.List;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.MCRPersistentIdentifierInscriber;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;


public class MCRURNObjectXPathInscriber extends MCRPersistentIdentifierInscriber<MCRDNBURN> {

    public MCRURNObjectXPathInscriber(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRDNBURN identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        String xpath = getProperties().get("Xpath");
        Document xml = obj.createXML();
        MCRNodeBuilder nb = new MCRNodeBuilder();
        try {
            nb.buildElement(xpath, identifier.asString(), xml);
            MCRBase object = new MCRObject(xml);
            MCRMetadataManager.update(object);
        } catch (IOException | JaxenException | MCRAccessException | MCRActiveLinkException e) {
            throw new MCRException("Error while inscribing URN to ", e);

        }
    }

    @Override
    public void removeIdentifier(MCRDNBURN identifier, MCRBase obj, String additional) {
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
    public boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        String xpath = getProperties().get("Xpath");
        Document xml = obj.createXML();
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> xp = xpfac.compile(xpath, Filters.element());
        return xp.evaluate(xml).size()>0;
    }
}
