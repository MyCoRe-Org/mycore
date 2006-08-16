/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.workflow;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRMetaAccessRule;
import org.mycore.datamodel.metadata.MCRMetaAddress;
import org.mycore.datamodel.metadata.MCRMetaBoolean;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaDate;
import org.mycore.datamodel.metadata.MCRMetaHistoryDate;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.datamodel.metadata.MCRMetaInstitutionName;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLink;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetaNumber;
import org.mycore.datamodel.metadata.MCRMetaPersonName;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * provides a wrappe for editor validation and MCRObject creation.
 * 
 * For a new MetaDataType, e.g. MCRMetaFooBaar, create a method
 * 
 * <pre>
 *           boolean checkMCRMetaFooBar(Element)
 * </pre>
 * 
 * use the following methods in that method to do common tasks on element
 * validation
 * <ul>
 * <li>checkMetaObject(Element,Class)</li>
 * <li>checkMetaObjectWithLang(Element,Class)</li>
 * <li>checkMetaObjectWithLangNotEmpty(Element,Class)</li>
 * <li>checkMetaObjectWithLinks(Element,Class)</li>
 * </ul>
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCREditorOutValidator {
    private List errorlog;

    private Document input;

    private MCRObjectID id;

    private static Logger LOGGER = Logger.getLogger(MCREditorOutValidator.class);

    private static final Map checkMethods;

    private static final ArrayList adduserlist;

    // The Access Manager
    protected static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    static {
        // save all check methods in a Map for later usage
        HashMap methods = new HashMap();
        Method[] m = MCREditorOutValidator.class.getDeclaredMethods();
        for (int i = 0; i < m.length; i++) {
            if (!m[i].getName().startsWith("checkMCR") || !((m[i].getParameterTypes().length == 1) && m[i].getParameterTypes()[0] == Element.class && m[i].getReturnType() == Boolean.TYPE)) {
                continue;
            }
            LOGGER.debug("adding Method " + m[i].getName());
            methods.put(m[i].getName().substring(5), m[i]);
        }
        checkMethods = Collections.unmodifiableMap(methods);
        // read the list of user add to ACL's
        adduserlist = new ArrayList();
        String inline = MCRConfiguration.instance().getString("MCR.AddUserPermissions", "read,write,delete");
        StringTokenizer st = new StringTokenizer(inline, ",");
        while (st.hasMoreTokens()) {
            adduserlist.add(st.nextToken());
        }
    }

    /**
     * instantiate the validator with the editor input <code>jdom_in</code>.
     * 
     * <code>id</code> will be set as the MCRObjectID for the resulting object
     * that can be fetched with <code>generateValidMyCoReObject()</code>
     * 
     * @param jdom_in
     *            editor input
     */
    public MCREditorOutValidator(Document jdom_in, MCRObjectID id) {
        this.errorlog = new ArrayList();
        this.input = jdom_in;
        this.id = id;
        byte[] xml = MCRUtils.getByteArray(input);
        System.out.println(new String(xml));
        checkObject();
        xml = MCRUtils.getByteArray(input);
        System.out.println(new String(xml));
    }

    /**
     * tries to generate a valid MCRObject as JDOM Document.
     * 
     * @return MCRObject
     */
    public Document generateValidMyCoReObject() {
        MCRObject obj = new MCRObject();
        try {
            // load the JDOM object
            byte[] xml = MCRUtils.getByteArray(input);
            obj.setFromXML(xml, true);
            Date curTime = new Date();
            obj.getService().setDate("createdate", curTime);
            obj.getService().setDate("modifydate", curTime);

            // return the XML tree
            input = obj.createXML();
        } catch (MCRException e) {
            errorlog.add(e.getMessage());

            Exception ex = e.getException();

            if (ex != null) {
                errorlog.add(ex.getMessage());
            }
        }
        return input;
    }

    /**
     * returns a List of Error log entries
     * 
     * @return log entries for the whole validation process
     */
    public List getErrorLog() {
        return errorlog;
    }

    /**
     * @param datatag
     */
    private boolean checkMetaTags(Element datatag) {
        String mcrclass = datatag.getAttributeValue("class");
        List datataglist = datatag.getChildren();
        Iterator datatagIt = datataglist.iterator();

        while (datatagIt.hasNext()) {
            Element datasubtag = (Element) datatagIt.next();
            if (checkMethods.containsKey(mcrclass)) {
                Method m = (Method) checkMethods.get(mcrclass);
                try {
                    Object returns = m.invoke(this, new Object[] { datasubtag });
                    if (!((Boolean) returns).booleanValue()) {
                        datatagIt.remove();
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Error while invoking " + m.getName(), e);
                } catch (IllegalAccessException e) {
                    LOGGER.warn("Error while invoking " + m.getName(), e);
                } catch (InvocationTargetException e) {
                    LOGGER.warn("Error while invoking " + m.getName(), e);
                }
            } else {
                LOGGER.warn("Tag <"+datatag.getName()+"> of type " + mcrclass + " has no validator defined, fallback to default behaviour");
                // try to create MCRMetaInterface instance
                try {
                    Class metaClass = Class.forName(mcrclass);
                    // just checks if class would validate this element
                    if (!checkMetaObject(datasubtag, metaClass)) {
                        datatagIt.remove();
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Failure while trying fallback. Class not found: " + mcrclass);
                }
            }
        }
        if (datatag.getChildren().size() == 0) {
            return false;
        }
        return true;
    }

    private boolean checkMetaObject(Element datasubtag, Class metaClass) {
        try {
            MCRMetaInterface test = (MCRMetaInterface) metaClass.newInstance();
            test.setFromDOM(datasubtag);

            if (!test.isValid()) {
                throw new MCRException("");
            }
        } catch (Exception e) {
            errorlog.add("Element " + datasubtag.getName() + " is not valid.");
            return false;
        }
        return true;
    }

    private boolean checkMetaObjectWithLang(Element datasubtag, Class metaClass) {
        if (datasubtag.getAttribute("lang") != null) {
            datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
        }
        return checkMetaObject(datasubtag, metaClass);
    }

    private boolean checkMetaObjectWithLangNotEmpty(Element datasubtag, Class metaClass) {
        String text = datasubtag.getTextNormalize();
        if ((text == null) || ((text = text.trim()).length() == 0)) {
            return false;
        }
        return checkMetaObjectWithLang(datasubtag, metaClass);
    }

    private boolean checkMetaObjectWithLinks(Element datasubtag, Class metaClass) {
        String href = datasubtag.getAttributeValue("href");
        if (href == null) {
            return false;
        }
        if (datasubtag.getAttribute("type") != null) {
            datasubtag.getAttribute("type").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        }

        if (datasubtag.getAttribute("href") != null) {
            datasubtag.getAttribute("href").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        }

        if (datasubtag.getAttribute("title") != null) {
            datasubtag.getAttribute("title").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        }

        if (datasubtag.getAttribute("label") != null) {
            datasubtag.getAttribute("label").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        }
        return checkMetaObject(datasubtag, metaClass);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaBoolean(Element datasubtag) {
        return checkMetaObject(datasubtag, MCRMetaBoolean.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaPersonName(Element datasubtag) {
        return checkMetaObjectWithLang(datasubtag, MCRMetaPersonName.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaInstitutionName(Element datasubtag) {
        return checkMetaObjectWithLang(datasubtag, MCRMetaInstitutionName.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaAddress(Element datasubtag) {
        return checkMetaObjectWithLang(datasubtag, MCRMetaAddress.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaNumber(Element datasubtag) {
        return checkMetaObjectWithLangNotEmpty(datasubtag, MCRMetaNumber.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaHistoryDate(Element datasubtag) {
        return checkMetaObjectWithLang(datasubtag, MCRMetaHistoryDate.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaDate(Element datasubtag) {
        return checkMetaObjectWithLangNotEmpty(datasubtag, MCRMetaDate.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaLinkID(Element datasubtag) {
        return checkMetaObjectWithLinks(datasubtag, MCRMetaLinkID.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaLink(Element datasubtag) {
        return checkMetaObjectWithLinks(datasubtag, MCRMetaLink.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaClassification(Element datasubtag) {
        String categid = datasubtag.getAttributeValue("categid");
        if (categid == null) {
            return false;
        }
        return checkMetaObject(datasubtag, MCRMetaClassification.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaISO8601Date(Element datasubtag) {
        return checkMetaObjectWithLangNotEmpty(datasubtag, MCRMetaISO8601Date.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaLangText(Element datasubtag) {
        return checkMetaObjectWithLangNotEmpty(datasubtag, MCRMetaLangText.class);
    }

    /**
     * @param datasubtag
     */
    private boolean checkMCRMetaAccessRule(Element datasubtag) {
        return checkMetaObjectWithLang(datasubtag, MCRMetaAccessRule.class);
    }

    /**
     * 
     */
    private void checkObject() {
        // add the namespaces (this is a workaround)
        org.jdom.Element root = input.getRootElement();
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        // set the schema
        String mcr_schema = "datamodel-" + id.getTypeId() + ".xsd";
        root.setAttribute("noNamespaceSchemaLocation", mcr_schema, org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        // check the label
        String label = root.getAttributeValue("label");
        if ((label == null) || ((label = label.trim()).length() == 0)) {
            root.setAttribute("label", id.getId());
        }
        // remove the path elements from the incoming
        org.jdom.Element pathes = root.getChild("pathes");
        if (pathes != null) {
            root.removeChildren("pathes");
        }
        org.jdom.Element structure = root.getChild("structure");
        if (structure == null) {
            root.addContent(new Element("structure"));
        } else {
            checkObjectStructure(structure);
        }
        Element metadata = root.getChild("metadata");
        checkObjectMetadata(metadata);
        org.jdom.Element service = root.getChild("service");
        checkObjectService(root, service);
    }

    /**
     * @param service
     */
    private void checkObjectService(Element root, Element service) {
        if (service == null) {
            service = new org.jdom.Element("service");
            root.addContent(service);
        }
        List servicelist = service.getChildren();
        if (servicelist == null) {
            setDefaultObjectACLs(service);
            return;
        }
        Iterator serviceIt = servicelist.iterator();

        boolean hasacls = false;
        while (serviceIt.hasNext()) {
            Element datatag = (Element) serviceIt.next();

            if (datatag.getName().equals("servdates")) {
                List servdatelist = datatag.getChildren();
                int servdatelistlen = servdatelist.size();
                for (int h = 0; h < servdatelistlen; h++) {
                    org.jdom.Element servdate = (org.jdom.Element) servdatelist.get(h);
                    checkMCRMetaISO8601Date(servdate);
                }

            }
            if (datatag.getName().equals("servacls")) {
                hasacls = true;
                List servacllist = datatag.getChildren();
                if (servacllist != null) {
                    int servacllistlen = servacllist.size();
                    for (int h = 0; h < servacllistlen; h++) {
                        org.jdom.Element servacl = (org.jdom.Element) servacllist.get(h);
                        checkMCRMetaAccessRule(servacl);
                    }
                }

            }
            if (datatag.getName().equals("servflags")) {
                List servflaglist = datatag.getChildren();
                if (servflaglist != null) {
                    int servflaglistlen = servflaglist.size();
                    for (int h = 0; h < servflaglistlen; h++) {
                        org.jdom.Element servflag = (org.jdom.Element) servflaglist.get(h);
                        checkMCRMetaLangText(servflag);
                    }
                }

            }
        }
        List li = AI.getPermissionsForID(id.getId());
        if ((li != null) && (li.size() > 0)) {
            hasacls = true;
        }
        if (!hasacls)
            setDefaultObjectACLs(service);
    }

    /**
     * The method add a default ACL-block.
     * 
     * @param service
     */
    private void setDefaultObjectACLs(org.jdom.Element service) {
        // Read stylesheet and add user
        InputStream aclxml = MCREditorOutValidator.class.getResourceAsStream("/editor_default_acls_" + id.getTypeId() + ".xml");
        if (aclxml == null) {
            LOGGER.warn("Can't find default object ACL file editor_default_acls_....xml.");
        } else {
            try {
                org.jdom.Document xml = (new org.jdom.input.SAXBuilder()).build(aclxml);
                org.jdom.Element nacls = xml.getRootElement().getChild("servacls");
                if (nacls != null) {
                    org.jdom.Element acls = (org.jdom.Element) nacls.clone();
                    List acllist = acls.getChildren();
                    if (acllist != null) {
                        for (int i = 0; i < acllist.size(); i++) {
                            org.jdom.Element acl = (org.jdom.Element) acllist.get(i);
                            if (acl != null) {
                                String perm = acl.getAttributeValue("permission");
                                if (adduserlist.contains(perm)) {
                                    org.jdom.Element condition = acl.getChild("condition");
                                    if (condition != null) {
                                        org.jdom.Element rootbool = condition.getChild("boolean");
                                        if (rootbool != null) {
                                            org.jdom.Element andbool = (org.jdom.Element) rootbool.clone();
                                            condition.removeContent();
                                            List orbool = andbool.getChildren("boolean");
                                            for (int j = 0; j < orbool.size(); j++) {
                                                org.jdom.Element oneorbool = (org.jdom.Element) orbool.get(j);
                                                org.jdom.Element firstcond = oneorbool.getChild("condition");
                                                if (firstcond != null) {
                                                    String field = firstcond.getAttributeValue("field");
                                                    if (field == null)
                                                        continue;
                                                    if (!field.equals("user") && !field.equals("group"))
                                                        continue;
                                                    oneorbool.addContent(getCurrentUser());
                                                }
                                            }
                                            condition.addContent(andbool);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    service.addContent(acls);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warn("Error while parsing file editor_default_acls_....xml.");
            }
        }
    }

    /**
     * The method build the condition element of the current user
     * 
     * @return a JDOM Element
     */
    private final org.jdom.Element getCurrentUser() {
        org.jdom.Element user = new org.jdom.Element("condition");
        user.setAttribute("field", "user");
        user.setAttribute("operator", "=");
        String thisuser = MCRSessionMgr.getCurrentSession().getCurrentUserID();
        user.setAttribute("value", thisuser);
        return user;
    }

    /**
     * @param metadata
     */
    private void checkObjectMetadata(Element metadata) {
        metadata.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);

        List metadatalist = metadata.getChildren();
        Iterator metaIt = metadatalist.iterator();

        while (metaIt.hasNext()) {
            Element datatag = (Element) metaIt.next();
            if (!checkMetaTags(datatag)) {
                // e.g. datatag is empty
                metaIt.remove();
            }
        }
    }

    /**
     * @param structure
     */
    private void checkObjectStructure(Element structure) {
        List structurelist = structure.getChildren();
        Iterator structIt = structurelist.iterator();

        while (structIt.hasNext()) {
            Element datatag = (Element) structIt.next();
            if (!checkMetaTags(datatag)) {
                // e.g. datatag is empty
                structIt.remove();
            }
        }
    }

    /**
     * The method add a default ACL-block.
     * 
     * @param service
     */
    protected static void setDefaultDerivateACLs(org.jdom.Element service, MCRObjectID id) {
        // Read stylesheet and add user
        InputStream aclxml = MCREditorOutValidator.class.getResourceAsStream("/editor_default_acls_" + id.getTypeId() + ".xml");
        if (aclxml == null) {
            LOGGER.warn("Can't find default derivate ACL file editor_default_acls_....xml.");
        } else {
            try {
                org.jdom.Document xml = (new org.jdom.input.SAXBuilder()).build(aclxml);
                org.jdom.Element acls = xml.getRootElement().getChild("servacls");
                if (acls != null) {
                    acls.detach();
                    service.addContent((org.jdom.Element) acls.clone());
                }
            } catch (Exception e) {
                LOGGER.warn("Error while parsing file editor_default_acls_....xml.");
            }
        }
    }

}
