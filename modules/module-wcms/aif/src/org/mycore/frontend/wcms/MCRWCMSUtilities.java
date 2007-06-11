package org.mycore.frontend.wcms;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;

public class MCRWCMSUtilities {
    final static String OBJIDPREFIX_WEBPAGE = "webpage:";
    final static int ALLTRUE = 1;
    final static int ONETRUE_ALLTRUE = 2;
    //private final static Logger LOGGER = Logger.getLogger("MCRWCMSUtilities"); 

    /*
     * public static boolean readAccess(String webpageID, String permission)
     * throws JDOMException, IOException { return getAccess(webpageID,
     * permission, ALLTRUE); }
     */
    
    
    public static boolean writeAccess(String webpageID) throws JDOMException, IOException {
        return getWriteAccessGeneral() && getAccess(webpageID, "write", ONETRUE_ALLTRUE);
    }
    
    public static org.w3c.dom.Document getWritableNavi() throws JDOMException, IOException {
        Element origNavi = new Element("root");
        origNavi.addContent(getNavi().getRootElement().detach());
        Document writableNavi = new Document(new Element("root"));
        
        System.out.println("######################################################");
        System.out.println("start to get writeable navi...");        
        System.out.println("######################################################");
    
        buildWritableNavi(origNavi, writableNavi);
    
        System.out.println("######################################################");
        System.out.println("finfished getting writeable navi...");        
        System.out.println("######################################################");
        
        return new DOMOutputter().output(writableNavi);
    }     

    protected static boolean getWriteAccessGeneral() {
        return MCRAccessManager.getAccessImpl().checkPermission("wcms-access");
    }

    private static boolean getAccess(String webpageID, String permission, int strategy) throws JDOMException, IOException {
        // get item as JDOM-Element
        final Document navi = getNavi();
        final String xpathExp = "//.[@href='" + webpageID + "']";
        XPath xpath = XPath.newInstance(xpathExp);
        Element item = (Element) xpath.selectSingleNode(navi);
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALLTRUE) {
            access = true;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            } while (item != null && access);
        } else if (strategy == ONETRUE_ALLTRUE) {
            access = false;
            do {
                access = itemAccess(permission, item, access);
                if (item.isRootElement()) 
                    item=null;
                else 
                    item = item.getParentElement();
            } while (item != null && !access);
        }
        return access;
    }

    private static boolean itemAccess(String permission, Element item, boolean access) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission))
            access = am.checkPermission(objID, permission);
        return access;
    }

    private static String getWebpageACLID(Element item) {
        return OBJIDPREFIX_WEBPAGE + getWebpageID(item);
    }

    private static String getWebpageID(Element item) {
        return item.getAttributeValue("href");
    }

    private static Document getNavi() throws JDOMException, IOException {
        final MCRConfiguration CONFIG = MCRConfiguration.instance();
        final File navFile = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
        final Document navigation = new SAXBuilder().build(navFile);
        return navigation;
    }

    private static void buildWritableNavi(Element origNavi, Document writableNavi) throws JDOMException, IOException {
        List childs = origNavi.getChildren();
        Iterator childIter = childs.iterator();
/*        System.out.println("######################################################");
        System.out.println("within recursive call, number of found items="+childs.size()+"..");        
        System.out.println("######################################################");*/
        int i =0;
        while (childIter.hasNext()) {
            i++;
/*            System.out.println("######################################################");
            System.out.println("within while children list, pos of childs="+i+"..");        
            System.out.println("######################################################");
  */          
            Element child = (Element) childIter.next();
            if (child.getAttributeValue("href")!=null) {
    /*            System.out.println("######################################################");
                System.out.println("child received from IteratorList, webpageID="+getWebpageID(child)+"..");
                System.out.println("######################################################");
*/                
                boolean access = writeAccess(getWebpageID(child));
        /*        System.out.println("######################################################");
                System.out.println("getWebpageID(child)="+getWebpageID(child)+"..");        
                System.out.println("######################################################");
                
                //boolean access = true;
                System.out.println("######################################################");
                System.out.println("access verified -> access="+access+"..");        
                System.out.println("######################################################");
  */              
                
                if (access) {
/*                    System.out.println("######################################################");
                    System.out.println("access allowed -> no recursive call...");        
                    System.out.println("######################################################");
                    */    
                    childIter.remove();
                    writableNavi.getRootElement().addContent(child);
                }
                else {
/*                    System.out.println("######################################################");
                    System.out.println("recall, because webpageID="+getWebpageID(child)+" was forbidden..");        
                    System.out.println("######################################################");
                    */
                    buildWritableNavi(child, writableNavi);
                }
            }

                
        }

        //return writableNavi;
    }
    
}












