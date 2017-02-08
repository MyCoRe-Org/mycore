package org.mycore.pi.urn.rest;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRDNBURN;

import java.net.URL;

import static org.mycore.common.MCRConstants.EPICURLITE_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

/**
 * Created by chi on 25.01.17.
 * @author Huu Chi Vu
 */
public class MCREpicurLite {
    private final MCRPIRegistrationInfo urn;

    private String password;

    private String login;

    private URL url;

    private boolean frontpage, primary;

    /**
     * Creates an {@link MCREpicurLite} where frontpage is set to false and primary set to true.
     */
    public MCREpicurLite(MCRPIRegistrationInfo urn) {
        this.urn = urn;
        this.frontpage = false;
        this.primary = true;
    }

    /**
     * Creates an {@link MCREpicurLite} where frontpage is set to false and primary set to true.
     *
     * @see MCREpicurLite#MCREpicurLite(MCRPIRegistrationInfo)
     */
    public MCREpicurLite(MCRPIRegistrationInfo urn, String login, String password) {
        this(urn);
        this.login = login;
        this.password = password;
    }

    /**
     * Creates the epicur lite xml.
     */
    public Document getEpicurLite() {
        //TODO support multiple url elements
        if (url == null) {
            return null;
        }

        Element epicurLite = new Element("epicurlite", EPICURLITE_NAMESPACE);
        epicurLite.addNamespaceDeclaration(XSI_NAMESPACE);
        epicurLite.setAttribute("schemaLocation",
                                "http://nbn-resolving.org/epicurlite http://nbn-resolving.org/schemas/epicurlite/1.0/epicurlite.xsd", XSI_NAMESPACE);
        Document epicurLiteDoc = new Document(epicurLite);

        // authentication information
        if (login != null && password != null) {
            Element login = new Element("login", EPICURLITE_NAMESPACE);
            Element password = new Element("password", EPICURLITE_NAMESPACE);
            login.setText(getLogin());
            password.setText(getPassword());
            epicurLite.addContent(login);
            epicurLite.addContent(password);
        }

        // urn element
        Element identifier = new Element("identifier", EPICURLITE_NAMESPACE);
        Element value = new Element("value", EPICURLITE_NAMESPACE);
        value.setText(urn.getIdentifier());
        epicurLite.addContent(identifier.addContent(value));

        // resource Element
        Element resource = new Element("resource", EPICURLITE_NAMESPACE);
        Element url = new Element("url", EPICURLITE_NAMESPACE);

        url.setText(getUrl().toString());

        Element primary = new Element("primary", EPICURLITE_NAMESPACE);
        primary.setText(String.valueOf(isPrimary()));

        Element frontpage = new Element("frontpage", EPICURLITE_NAMESPACE);
        frontpage.setText(String.valueOf(isFrontpage()));
        resource.addContent(url);
        resource.addContent(primary);
        resource.addContent(frontpage);

        epicurLite.addContent(resource);

        return epicurLiteDoc;
    }

    /**
     * Get the url the urn is pointing to.
     *
     * @return url
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * Set the url the urn is pointing to.
     *
     * @param url url the urn is pointing to
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the urn
     */
    public MCRPIRegistrationInfo getUrn() {
        return urn;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @return the frontpage
     */
    public boolean isFrontpage() {
        return frontpage;
    }

    /**
     * @param frontpage the frontpage to set
     */
    public void setFrontpage(boolean frontpage) {
        this.frontpage = frontpage;
    }

    /**
     * @return the primary
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * @param primary the primary to set
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    @Override
    public String toString() {
        return urn + "|" + url.toString();
    }
}
