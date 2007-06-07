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
	final static String OBJIDPREFIX_WEBPAGE= "webpage:";
	final static int ALLTRUE = 1;
	final static int ONETRUE_ALLTRUE = 2;
	/*
	public static boolean readAccess(String webpageID, String permission) throws JDOMException, IOException {
		return getAccess(webpageID, permission, ALLTRUE);
	}
	*/
	public static boolean writeAccess(String webpageID, String permission) throws JDOMException, IOException {
		return getWriteAccessGeneral() && getAccess(webpageID, permission, ONETRUE_ALLTRUE);
	}

	protected static boolean getWriteAccessGeneral(){
		return MCRAccessManager.getAccessImpl().checkPermission("wcms-access");
	}
	
	private static boolean getAccess(String webpageID, String permission, int strategy) throws JDOMException, IOException {
		// get item as JDOM-Element
		final Document navi = getNavi();
		String xpathExp = "//item[@href='"+webpageID+"']";
		XPath xpath = XPath.newInstance(xpathExp); 
		Element item = (Element)xpath.selectSingleNode(navi);
		
		// check permission according to $strategy
		boolean access=false;
		if (strategy==ALLTRUE) {
			access = true;
			do {
				access = getItemAccess(permission, item, access);
				item = item.getParentElement();
			} while (item!=null && access);
		} 
		else if (strategy==ONETRUE_ALLTRUE){
			access = false;
			do {
				access = getItemAccess(permission, item, access);
				item = item.getParentElement();
			} while (item!=null && !access);
			
			
		}
		return access;
	}

	private static boolean getItemAccess(String permission, Element item, boolean access) {
		MCRAccessInterface am = MCRAccessManager.getAccessImpl();
		String itemID = item.getAttributeValue("href");
		String objID = OBJIDPREFIX_WEBPAGE+itemID;
		if (am.hasRule(objID, permission)) 
			access = am.checkPermission(objID,permission);
		return access;
	}
	
	private static Document getNavi() throws JDOMException, IOException {
		final MCRConfiguration CONFIG = MCRConfiguration.instance();
		final File navFile = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
		final Document navigation = new SAXBuilder().build(navFile);
		return navigation;
	}
}