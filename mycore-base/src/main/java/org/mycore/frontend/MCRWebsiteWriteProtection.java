package org.mycore.frontend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;

public final class MCRWebsiteWriteProtection {
    private static final String FS = System.getProperty("file.separator");

    private static final String CONFIG_FOLDER_PATH = MCRConfiguration.instance().getString("MCR.datadir") + FS
        + "config";

    private static final String CONFIG_FILE_PATH = new String(CONFIG_FOLDER_PATH + FS
        + "config-writeProtectionWebsite.xml");

    private static final File CONFIG_FILE = new File(CONFIG_FILE_PATH);

    private static long cacheInitTime = 0;

    private static Element configCache = null;

    private MCRWebsiteWriteProtection() {
        //do not allow instantiation
    }

    /**
     * Checks if website protection is currently active.
     * If current user is super user this method always returns false.
     * 
     * @return true if write access is currently active, false if not
     */
    public static boolean isActive() {
        // if superuser is online, return false
        String superUser = MCRSystemUserInformation.getSuperUserInstance().getUserID();
        if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID().equals(superUser)) {
            return false;
        }
        // init, if impossible return false
        Element config = getConfiguration();
        if (config == null) {
            return false;
        }
        // return value contained in config
        String protection = config.getChildTextTrim("protectionEnabled");
        return Boolean.valueOf(protection);
    }

    public static org.w3c.dom.Document getMessage() throws JDOMException, IOException {
        Element config = getConfiguration();
        if (config == null) {
            return new DOMOutputter().output(new Document());
        } else {
            Element messageElem = config.getChild("message");
            Document message = new Document((Element) messageElem.clone());
            return new DOMOutputter().output(message);
        }
    }

    private static Element getConfiguration() {
        // try to get file
        File configFolder = new File(CONFIG_FOLDER_PATH);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        // file exist?, return it's content
        if (CONFIG_FILE.exists()) {
            Element config = null;
            // try to get from cache
            if (cacheValid()) {
                config = configCache;
            } else {
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

    private static void setConfiguration(Element configXML) {
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
        configCache = configXML;
        cacheInitTime = System.currentTimeMillis();
    }

    private static Element configToJDOM(boolean protection, String message) {
        Element xml = new Element("config-writeProtectionWebsite");
        xml.addContent(new Element("protectionEnabled").setText(Boolean.toString(protection)));
        xml.addContent(new Element("message").setText(message));
        return xml;
    }

    // to be used by cli
    public static void activate() {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("true");
        setConfiguration(config);
    }

    // to be used by cli
    public static void activate(String message) {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("true");
        config.getChild("message").setText(message);
        setConfiguration(config);
    }

    // to be used by cli
    public static void deactivate() {
        // set param in file to false
        Element config = getConfiguration();
        config.getChild("protectionEnabled").setText("false");
        setConfiguration(config);
    }

    public static boolean printInfoPageIfNoAccess(HttpServletRequest request, HttpServletResponse response,
        String baseURL) throws IOException {
        if (MCRWebsiteWriteProtection.isActive()) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            String pageURL = baseURL + MCRConfiguration.instance().getString("MCR.WriteProtectionWebsite.ErrorPage");
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
    private static boolean cacheValid() {
        return !(configCache == null || cacheInitTime < CONFIG_FILE.lastModified());
    }

}
