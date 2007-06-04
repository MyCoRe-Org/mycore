package org.mycore.frontend.wcms;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;


public class MCRWCMSUtilities {
	final static String OBJIDPREFIX= "webpage:";
	
	public static boolean readAccess(String webpageID, String permission) throws JDOMException, IOException {
		return getAccess(webpageID, permission);
	}
	
	public static boolean writeAccess(String webpageID, String permission) throws JDOMException, IOException {
		return getWriteAccessGeneral() && readAccess(webpageID, permission);
	}

	private static boolean getAccess(String webpageID, String permission) throws JDOMException, IOException {
		
		// get item as JDOM-Element
		final Document navi = getNavi();
		String xpathExp = "//item[@href='"+webpageID+"']";
		XPath xpath = XPath.newInstance(xpathExp); 
		Element item = (Element)xpath.selectSingleNode(navi);
		
		// check permission != false of each parent item and of current one
		boolean access = true;
		MCRAccessInterface am = MCRAccessManager.getAccessImpl();
		do {
			String itemID = item.getAttributeValue("href");
			String objID = OBJIDPREFIX+itemID;
			if (am.hasRule(objID, permission)) 
				access = am.checkPermission(objID,permission);
			item = item.getParentElement();
		} while (item!=null && access);
		
		return access;
	}

	private static boolean getWriteAccessGeneral(){
		return MCRAccessManager.getAccessImpl().checkPermission("wcms-access");
	}
	
	private static Document getNavi() throws JDOMException, IOException {
		final MCRConfiguration CONFIG = MCRConfiguration.instance();
		final File navFile = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
		final Document navigation = new SAXBuilder().build(navFile);
		return navigation;
	}
}