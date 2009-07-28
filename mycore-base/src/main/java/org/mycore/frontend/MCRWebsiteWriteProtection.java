package org.mycore.frontend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;

public final class MCRWebsiteWriteProtection {
    static final private String FS = System.getProperty("file.separator");

    static MCRConfiguration MCR_CONFIG = MCRConfiguration.instance();

    static final private String CONFIG_FOLDER_PATH = MCR_CONFIG.getString("MCR.datadir") + FS + "config";

    static final private String CONFIG_FILE_PATH = new String(CONFIG_FOLDER_PATH + FS + "config-writeProtectionWebsite.xml");

    static final private File CONFIG_FILE = new File(CONFIG_FILE_PATH);

    static private long CONFIG_CACHE_INITTIME = 0;

    static private Element CONFIG_CACHE = null;

    /**
     * speed up the check
     * 
     * @return true if write access is currently active, false if not
     */
    public final static boolean isActive() {
        // if superuser is online, return false
        String superUser = MCR_CONFIG.getString("MCR.Users.Superuser.UserName");
        if (MCRSessionMgr.getCurrentSession().getCurrentUserID().equals(superUser))
            return false;
        // init, if impossible return false
        Element config = getConfiguration();
        if (config == null)
            return false;
        // return value contained in config
        String protection = config.getChildTextTrim("protectionEnabled");
        return Boolean.valueOf(protection);
    }

    public final static org.w3c.dom.Document getMessage() throws JDOMException, IOException {
        Element config = getConfiguration();
        if (config == null)
            return new DOMOutputter().output(new Document());
        else {
            Element messageElem = config.getChild("message");
            Document message = new Document((Element) messageElem.clone());
            return new DOMOutputter().output(message);
        }
    }

    private final static Element getConfiguration() {
        // try to get file
        File configFolder = new File(CONFIG_FOLDER_PATH);
        if (!configFolder.exists())
            configFolder.mkdir();
        // file exist?, return it's content
        if (CONFIG_FILE.exists()) {
            Element config = null;
            // try to get from cache
            if (cacheValid())
                config = CONFIG_CACHE;
            // parse it
            else {
                SAXBuilder builder = new SAXBuilder();
                try {
                    config = builder.build(CONFIG_FILE).getRootElement();
                    // update cache
                    updateCache(config);
                } catch (JDOMException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return config;
        } else {
            // create XML
            Element config = configToJDOM(false, " ");
            setConfiguration(config);
            return config;
        }
    }

    private final static void setConfiguration(Element configXML) {
        try {
            // save
            XMLOutputter xmlOut = new XMLOutputter();
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            xmlOut.output(configXML, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateCache(configXML);
    }

    /**
     * @param configXML
     */
    private static void updateCache(Element configXML) {
        CONFIG_CACHE = configXML;
        CONFIG_CACHE_INITTIME = System.currentTimeMillis();
    }

    private static Element configToJDOM(boolean protection, String message) {
        Element xml = new Element("config-writeProtectionWebsite");
        xml.addContent(new Element("protectionEnabled").setText(Boolean.toString(protection)));
        xml.addContent(new Element("message").setText(message));
        return xml;
    }

    // to be used by cli
    public final static void activate() {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("true");
        setConfiguration(config);
    }

    // to be used by cli
    public final static void activate(String message) {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("true");
        config.getChild("message").setText(message);
        setConfiguration(config);
    }

    // to be used by cli
    public final static void deactivate() {
        // set param in file to false
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("false");
        setConfiguration(config);
    }

    public final static boolean printInfoPageIfNoAccess(HttpServletRequest request, HttpServletResponse response, String baseURL) throws IOException {
        if (MCRWebsiteWriteProtection.isActive()) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            String pageURL = baseURL + MCR_CONFIG.getString("MCR.WriteProtectionWebsite.ErrorPage");
            response.sendRedirect(response.encodeRedirectURL(pageURL));
            return true;
        }
        return false;
    }

    /**
     * Verifies if the cache of configuration is valid.
     * 
     * @return true if valid, false if note
     */
    private final static boolean cacheValid() {
        if ((CONFIG_CACHE == null) || (CONFIG_CACHE_INITTIME < CONFIG_FILE.lastModified()))
            return false;
        else
            return true;
    }

}