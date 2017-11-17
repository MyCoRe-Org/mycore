/**
 * 
 */
package org.mycore.user2.utils;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.user2.MCRRole;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRRoleTransformer {

    private static final String ROLE_ELEMENT_NAME = "role";

    private static final JAXBContext JAXB_CONTEXT = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(MCRRole.class.getPackage().getName(), MCRRole.class.getClassLoader());
        } catch (JAXBException e) {
            throw new MCRException("Could not instantiate JAXBContext.", e);
        }
    }

    /**
     * Builds an xml element containing all information on the given role.
     */
    public static Document buildExportableXML(MCRRole role) {
        MCRJAXBContent<MCRRole> content = new MCRJAXBContent<>(JAXB_CONTEXT, role);
        try {
            return content.asXML();
        } catch (SAXParseException | JDOMException | IOException e) {
            throw new MCRException("Exception while transforming MCRRole " + role.getName() + " to JDOM document.", e);
        }
    }

    /**
     * Builds an MCRRole instance from the given element.
     * @param element as generated by {@link #buildExportableXML(MCRRole)}. 
     */
    public static MCRRole buildMCRRole(Element element) {
        if (!element.getName().equals(ROLE_ELEMENT_NAME)) {
            throw new IllegalArgumentException("Element is not a mycore role element.");
        }
        try {
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            return (MCRRole) unmarshaller.unmarshal(new JDOMSource(element));
        } catch (JAXBException e) {
            throw new MCRException("Exception while transforming Element to MCRUser.", e);
        }
    }

}
