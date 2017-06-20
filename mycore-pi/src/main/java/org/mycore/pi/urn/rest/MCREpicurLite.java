package org.mycore.pi.urn.rest;

import static org.mycore.common.MCRConstants.EPICURLITE_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.net.URL;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.MCRPIRegistrationInfo;

/**
 * Created by chi on 25.01.17.
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCREpicurLite {
    private final MCRPIRegistrationInfo urn;

    private final URL url;

    private UsernamePasswordCredentials credentials;

    private boolean isFrontpage = false;

    private boolean isPrimary = true;

    private MCREpicurLite(MCRPIRegistrationInfo urn, URL url) {
        this.urn = urn;
        this.url = url;
    }

    public static MCREpicurLite instance(MCRPIRegistrationInfo urn, URL url) {
        return new MCREpicurLite(urn, url);
    }

    /**
     * Creates the epicur lite xml.
     */
    public Document toXML() {
        //TODO support multiple url elements

        Element epicurLite = newEpicureElement("epicurlite");
        epicurLite.addNamespaceDeclaration(XSI_NAMESPACE);
        epicurLite.setAttribute("schemaLocation",
            "http://nbn-resolving.org/epicurlite http://nbn-resolving.org/schemas/epicurlite/1.0/epicurlite.xsd",
            XSI_NAMESPACE);
        Document epicurLiteDoc = new Document(epicurLite);

        // authentication information
        if (credentials != null) {
            Element login = newEpicureElement("login");
            Element password = newEpicureElement("password");
            login.setText(credentials.getUserName());
            password.setText(credentials.getPassword());
            epicurLite.addContent(login);
            epicurLite.addContent(password);
        }

        // urn element
        Element identifier = newEpicureElement("identifier");
        Element value = newEpicureElement("value");
        value.setText(urn.getIdentifier());
        epicurLite.addContent(identifier.addContent(value));

        // resource Element
        Element resource = newEpicureElement("resource");
        Element urlElem = newEpicureElement("url");

        urlElem.setText(url.toString());

        Element primary = newEpicureElement("primary");
        primary.setText(String.valueOf(isPrimary));

        Element frontpage = newEpicureElement("frontpage");
        frontpage.setText(String.valueOf(isFrontpage));
        resource.addContent(urlElem);
        resource.addContent(primary);
        resource.addContent(frontpage);

        epicurLite.addContent(resource);

        return epicurLiteDoc;
    }

    public String asXMLString() {
        return new XMLOutputter(
            Format.getPrettyFormat()).outputString(toXML());
    }

    private Element newEpicureElement(String epicurlite) {
        return new Element(epicurlite, EPICURLITE_NAMESPACE);
    }

    public MCREpicurLite setCredentials(UsernamePasswordCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * @param frontpage the frontpage to set
     */
    public MCREpicurLite setFrontpage(boolean frontpage) {
        isFrontpage = frontpage;
        return this;
    }

    /**
     * @param primary the primary to set
     */
    public MCREpicurLite setPrimary(boolean primary) {
        isPrimary = primary;
        return this;
    }

    @Override
    public String toString() {
        return urn.getIdentifier() + "|" + url.toString();
    }
}
