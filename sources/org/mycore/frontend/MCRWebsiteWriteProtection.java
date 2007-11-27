package org.mycore.frontend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRWebsiteWriteProtection {
    static String FS = System.getProperty("file.separator");

    static MCRConfiguration MCR_CONFIG = MCRConfiguration.instance();

    static private String CONFIG_FOLDER_PATH = MCR_CONFIG.getString("MCR.datadir") + FS + "config";

    static private String CONFIG_FILE_PATH = new String(CONFIG_FOLDER_PATH + FS + "config-writeProtectionWebsite.xml");

    /**
     * speed up the check
     * 
     * @return true if write access is currently active, false if not
     */
    public static boolean isActive() {
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

    public static org.w3c.dom.Document getMessage() throws JDOMException, IOException {
        Element config = getConfiguration();
        if (config == null)
            return new DOMOutputter().output(new Document());
        else {
            Element messageElem = config.getChild("message");
            Document message = new Document((Element) messageElem.clone());
            return new DOMOutputter().output(message);
        }
    }

    private static Element getConfiguration() {
        // try to get file
        File configFolder = new File(CONFIG_FOLDER_PATH);
        if (!configFolder.exists())
            configFolder.mkdir();
        // file exist?, return it's content
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            SAXBuilder builder = new SAXBuilder();
            Element config = null;
            try {
                config = builder.build(configFile).getRootElement();
            } catch (JDOMException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
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
            File configFile = new File(CONFIG_FILE_PATH);
            FileOutputStream fos = new FileOutputStream(configFile);
            xmlOut.output(configXML, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void verifyAccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (MCRWebsiteWriteProtection.isActive()) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            String pageURL = MCR_CONFIG.getString("MCR.baseurl") + MCR_CONFIG.getString("MCR.WriteProtectionWebsite.ErrorPage");
            response.sendRedirect(response.encodeRedirectURL(pageURL));
        }

    }

}
