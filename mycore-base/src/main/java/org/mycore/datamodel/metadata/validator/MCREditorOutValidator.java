/*
 * $Revision: 1 $ $Date: 08.05.2009 15:51:37 $
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

package org.mycore.datamodel.metadata.validator;

import static org.jdom2.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRMetaAccessRule;
import org.mycore.datamodel.metadata.MCRMetaAddress;
import org.mycore.datamodel.metadata.MCRMetaBoolean;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
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
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 * @version $Revision: 1 $ $Date: 08.05.2009 15:51:37 $
 */
public class MCREditorOutValidator {

    private static Logger LOGGER = LogManager.getLogger();

    private static Map<String, MCREditorMetadataValidator> VALIDATOR_MAP = getValidatorMap();

    private static Map<String, Class<? extends MCRMetaInterface>> CLASS_MAP = new HashMap<String, Class<? extends MCRMetaInterface>>();

    private static final String CONFIG_PREFIX = "MCR.EditorOutValidator.";

    private static final SAXBuilder SAX_BUILDER = new org.jdom2.input.SAXBuilder();

    private Document input;

    private MCRObjectID id;

    private List<String> errorlog;

    /**
     * instantiate the validator with the editor input <code>jdom_in</code>.
     * 
     * <code>id</code> will be set as the MCRObjectID for the resulting object
     * that can be fetched with <code>generateValidMyCoReObject()</code>
     * 
     * @param jdom_in
     *            editor input
     */
    public MCREditorOutValidator(Document jdom_in, MCRObjectID id) throws JDOMException, IOException {
        errorlog = new ArrayList<String>();
        input = jdom_in;
        this.id = id;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XML before validation:\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(input));
        }
        checkObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XML after validation:\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(input));
        }
    }

    /**
     * tries to generate a valid MCRObject as JDOM Document.
     * 
     * @return MCRObject
     */
    public Document generateValidMyCoReObject() throws JDOMException, SAXParseException, IOException {
        MCRObject obj;
        // load the JDOM object
        XPathFactory.instance()
            .compile("/mycoreobject/*/*/*/@editor.output", Filters.attribute())
            .evaluate(input)
            .forEach(Attribute::detach);
        try {
            byte[] xml = new MCRJDOMContent(input).asByteArray();
            obj = new MCRObject(xml, true);
        } catch (SAXParseException e) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            LOGGER.warn("Failure while parsing document:\n" + xout.outputString(input));
            throw e;
        }
        Date curTime = new Date();
        obj.getService().setDate("modifydate", curTime);

        // return the XML tree
        input = obj.createXML();
        return input;
    }

    /**
     * returns a List of Error log entries
     * 
     * @return log entries for the whole validation process
     */
    public List<String> getErrorLog() {
        return errorlog;
    }

    private static Map<String, MCREditorMetadataValidator> getValidatorMap() {
        Map<String, MCREditorMetadataValidator> map = new HashMap<String, MCREditorMetadataValidator>();
        map.put(MCRMetaBoolean.class.getSimpleName(), getObjectCheckInstance(MCRMetaBoolean.class));
        map.put(MCRMetaPersonName.class.getSimpleName(), getObjectCheckWithLangInstance(MCRMetaPersonName.class));
        map.put(MCRMetaInstitutionName.class.getSimpleName(),
            getObjectCheckWithLangInstance(MCRMetaInstitutionName.class));
        map.put(MCRMetaAddress.class.getSimpleName(), new MCRMetaAdressCheck());
        map.put(MCRMetaNumber.class.getSimpleName(), getObjectCheckWithLangNotEmptyInstance(MCRMetaNumber.class));
        map.put(MCRMetaLinkID.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaLinkID.class));
        map.put(MCRMetaDerivateLink.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaDerivateLink.class));
        map.put(MCRMetaLink.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaLink.class));
        map.put(MCRMetaISO8601Date.class.getSimpleName(),
            getObjectCheckWithLangNotEmptyInstance(MCRMetaISO8601Date.class));
        map.put(MCRMetaLangText.class.getSimpleName(), getObjectCheckWithLangNotEmptyInstance(MCRMetaLangText.class));
        map.put(MCRMetaAccessRule.class.getSimpleName(), getObjectCheckInstance(MCRMetaAccessRule.class));
        map.put(MCRMetaClassification.class.getSimpleName(), new MCRMetaClassificationCheck());
        map.put(MCRMetaHistoryDate.class.getSimpleName(), new MCRMetaHistoryDateCheck());
        Map<String, String> props = MCRConfiguration.instance().getPropertiesMap(CONFIG_PREFIX + "class.");
        for (Entry<String, String> entry : props.entrySet()) {
            try {
                String className = entry.getKey();
                className = className.substring(className.lastIndexOf('.') + 1);
                LOGGER.info("Adding Validator " + entry.getValue() + " for class " + className);
                @SuppressWarnings("unchecked")
                Class<? extends MCREditorMetadataValidator> cl = (Class<? extends MCREditorMetadataValidator>) Class
                    .forName(entry.getValue());
                map.put(className, cl.newInstance());
            } catch (Exception e) {
                final String msg = "Cannot instantiate " + entry.getValue() + " as validator for class "
                    + entry.getKey();
                LOGGER.error(msg);
                throw new MCRException(msg, e);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends MCRMetaInterface> getClass(String mcrclass) throws ClassNotFoundException {
        Class<? extends MCRMetaInterface> clazz = CLASS_MAP.get(mcrclass);
        if (clazz == null) {
            clazz = (Class<? extends MCRMetaInterface>) Class.forName("org.mycore.datamodel.metadata." + mcrclass);
            CLASS_MAP.put(mcrclass, clazz);
        }
        return clazz;
    }

    public static String checkMetaObject(Element datasubtag, Class<? extends MCRMetaInterface> metaClass,
        boolean keepLang) {
        if (!keepLang) {
            datasubtag.removeAttribute("lang", XML_NAMESPACE);
        }
        MCRMetaInterface test = null;
        try {
            test = metaClass.newInstance();
        } catch (Exception e) {
            throw new MCRException("Could not instantiate " + metaClass.getCanonicalName());
        }
        test.setFromDOM(datasubtag);
        test.validate();
        return null;
    }

    public static String checkMetaObjectWithLang(Element datasubtag, Class<? extends MCRMetaInterface> metaClass) {
        if (datasubtag.getAttribute("lang") != null) {
            datasubtag.getAttribute("lang").setNamespace(XML_NAMESPACE);
            LOGGER.warn("namespace add for xml:lang attribute in " + datasubtag.getName());
        }
        return checkMetaObject(datasubtag, metaClass, true);
    }

    public static String checkMetaObjectWithLangNotEmpty(Element datasubtag,
        Class<? extends MCRMetaInterface> metaClass) {
        String text = datasubtag.getTextTrim();
        if (text == null || text.length() == 0) {
            return "Element " + datasubtag.getName() + " has no text.";
        }
        return checkMetaObjectWithLang(datasubtag, metaClass);
    }

    public static String checkMetaObjectWithLinks(Element datasubtag, Class<? extends MCRMetaInterface> metaClass) {
        if (datasubtag.getAttributeValue("href") == null
            && datasubtag.getAttributeValue("href", XLINK_NAMESPACE) == null) {
            return datasubtag.getName() + " has no href attribute defined";
        }
        if (datasubtag.getAttribute("xtype") != null) {
            datasubtag.getAttribute("xtype").setNamespace(XLINK_NAMESPACE).setName("type");
        } else if (datasubtag.getAttribute("type") != null
            && datasubtag.getAttribute("type", XLINK_NAMESPACE) == null) {
            datasubtag.getAttribute("type").setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:type attribute in " + datasubtag.getName());
        }
        if (datasubtag.getAttribute("href") != null) {
            datasubtag.getAttribute("href").setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:href attribute in " + datasubtag.getName());
        }

        if (datasubtag.getAttribute("title") != null) {
            datasubtag.getAttribute("title").setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:title attribute in " + datasubtag.getName());
        }

        if (datasubtag.getAttribute("label") != null) {
            datasubtag.getAttribute("label").setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:label attribute in " + datasubtag.getName());
        }
        return checkMetaObject(datasubtag, metaClass, false);
    }

    static MCREditorMetadataValidator getObjectCheckInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> MCREditorOutValidator.checkMetaObject(datasubtag, clazz, false);
    }

    static MCREditorMetadataValidator getObjectCheckWithLangInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> MCREditorOutValidator.checkMetaObjectWithLang(datasubtag, clazz);
    }

    static MCREditorMetadataValidator getObjectCheckWithLangNotEmptyInstance(
        final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> MCREditorOutValidator.checkMetaObjectWithLangNotEmpty(datasubtag, clazz);
    }

    static MCREditorMetadataValidator getObjectCheckWithLinksInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> MCREditorOutValidator.checkMetaObjectWithLinks(datasubtag, clazz);
    }

    static class MCRMetaHistoryDateCheck implements MCREditorMetadataValidator {
        public String checkDataSubTag(Element datasubtag) {
            List<Element> children = datasubtag.getChildren("text");
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                String text = child.getTextTrim();
                if (text == null || text.length() == 0) {
                    datasubtag.removeContent(child);
                    i--;
                    continue;
                }
                if (child.getAttribute("lang") != null) {
                    child.getAttribute("lang").setNamespace(XML_NAMESPACE);
                    LOGGER.warn("namespace add for xml:lang attribute in " + datasubtag.getName());
                }
            }
            if (children.size() == 0) {
                return "history date is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaHistoryDate.class);
        }
    }

    static class MCRMetaClassificationCheck implements MCREditorMetadataValidator {
        public String checkDataSubTag(Element datasubtag) {
            String categid = datasubtag.getAttributeValue("categid");
            if (categid == null) {
                return "Attribute categid is empty";
            }
            return checkMetaObject(datasubtag, MCRMetaClassification.class, false);
        }
    }

    static class MCRMetaAdressCheck implements MCREditorMetadataValidator {
        public String checkDataSubTag(Element datasubtag) {
            if (datasubtag.getChildren().size() == 0) {
                return "adress is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaAddress.class);
        }
    }

    static class MCRMetaPersonNameCheck implements MCREditorMetadataValidator {
        public String checkDataSubTag(Element datasubtag) {
            if (datasubtag.getChildren().size() == 0) {
                return "person name is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaAddress.class);
        }
    }

    /**
     * @throws IOException 
     * @throws JDOMException 
     * 
     */
    private void checkObject() throws JDOMException, IOException {
        // add the namespaces (this is a workaround)
        org.jdom2.Element root = input.getRootElement();
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        // set the schema
        String mcr_schema = "datamodel-" + id.getTypeId() + ".xsd";
        root.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        // check the label
        String label = root.getAttributeValue("label");
        if (label == null || (label = label.trim()).length() == 0) {
            root.setAttribute("label", id.toString());
        }
        // remove the path elements from the incoming
        org.jdom2.Element pathes = root.getChild("pathes");
        if (pathes != null) {
            root.removeChildren("pathes");
        }
        org.jdom2.Element structure = root.getChild("structure");
        if (structure == null) {
            root.addContent(new Element("structure"));
        } else {
            checkObjectStructure(structure);
        }
        Element metadata = root.getChild("metadata");
        checkObjectMetadata(metadata);
        org.jdom2.Element service = root.getChild("service");
        checkObjectService(root, service);
    }

    /**
     * @param datatag
     */
    private boolean checkMetaTags(Element datatag) {
        String mcrclass = datatag.getAttributeValue("class");
        List<Element> datataglist = datatag.getChildren();
        Iterator<Element> datatagIt = datataglist.iterator();

        while (datatagIt.hasNext()) {
            Element datasubtag = (Element) datatagIt.next();
            MCREditorMetadataValidator validator = VALIDATOR_MAP.get(mcrclass);
            String returns = null;
            if (validator != null) {
                returns = validator.checkDataSubTag(datasubtag);
            } else {
                LOGGER.warn("Tag <" + datatag.getName() + "> of type " + mcrclass
                    + " has no validator defined, fallback to default behaviour");
                // try to create MCRMetaInterface instance
                try {
                    Class<? extends MCRMetaInterface> metaClass = getClass(mcrclass);
                    // just checks if class would validate this element
                    returns = checkMetaObject(datasubtag, metaClass, true);
                } catch (ClassNotFoundException e) {
                    throw new MCRException("Failure while trying fallback. Class not found: " + mcrclass, e);
                }
            }
            if (returns != null) {
                datatagIt.remove();
                final String msg = datatag.getName() + ": " + returns;
                errorlog.add(msg);
            }
        }
        return datatag.getChildren().size() != 0;
    }

    /**
     * @param service
     * @throws IOException 
     * @throws JDOMException 
     */
    private void checkObjectService(Element root, Element service) throws JDOMException, IOException {
        if (service == null) {
            service = new org.jdom2.Element("service");
            root.addContent(service);
        }
        List<Element> servicelist = service.getChildren();
        boolean hasacls = false;
        for (Element datatag : servicelist) {
            checkMetaTags(datatag);
        }
        Collection<String> li = MCRAccessManager.getPermissionsForID(id.toString());
        if (li != null && !li.isEmpty()) {
            hasacls = true;
        }
        if (service.getChild("servacls") == null && !hasacls) {
            setDefaultObjectACLs(service);
        }
    }

    /**
     * The method add a default ACL-block.
     * 
     * @param service
     * @throws IOException 
     * @throws JDOMException 
     */
    private void setDefaultObjectACLs(org.jdom2.Element service) throws JDOMException, IOException {
        if (!MCRConfiguration.instance().getBoolean("MCR.Access.AddObjectDefaultRule", true)) {
            LOGGER.info("Adding object default acl rule is disabled.");
            return;
        }
        String resourcetype = "/editor_default_acls_" + id.getTypeId() + ".xml";
        String resourcebase = "/editor_default_acls_" + id.getBase() + ".xml";
        // Read stylesheet and add user
        InputStream aclxml = MCREditorOutValidator.class.getResourceAsStream(resourcebase);
        if (aclxml == null) {
            aclxml = MCREditorOutValidator.class.getResourceAsStream(resourcetype);
            if (aclxml == null) {
                LOGGER.warn("Can't find default object ACL file " + resourcebase.substring(1) + " or "
                    + resourcetype.substring(1));
                String resource = "/editor_default_acls.xml"; // fallback
                aclxml = MCREditorOutValidator.class.getResourceAsStream(resource);
                if (aclxml == null) {
                    return;
                }
            }
        }
        Document xml = SAX_BUILDER.build(aclxml);
        Element acls = xml.getRootElement().getChild("servacls");
        if (acls == null) {
            return;
        }
        for (Element acl : (Iterable<Element>) acls.getChildren()) {
            Element condition = acl.getChild("condition");
            if (condition == null) {
                continue;
            }
            Element rootbool = condition.getChild("boolean");
            if (rootbool == null) {
                continue;
            }
            for (Element orbool : (Iterable<Element>) rootbool.getChildren("boolean")) {
                for (Element firstcond : (Iterable<Element>) orbool.getChildren("condition")) {
                    if (firstcond == null) {
                        continue;
                    }
                    String value = firstcond.getAttributeValue("value");
                    if (value == null) {
                        continue;
                    }
                    if (value.equals("$CurrentUser")) {
                        String thisuser = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
                        firstcond.setAttribute("value", thisuser);
                        continue;
                    }
                    if (value.equals("$CurrentGroup")) {
                        throw new MCRException(
                            "The parameter $CurrentGroup in default ACLs is no more supported since MyCoRe 2014.06 because it is not supported in Servlet API 3.0");
                    }
                    int i = value.indexOf("$CurrentIP");
                    if (i != -1) {
                        String thisip = MCRSessionMgr.getCurrentSession().getCurrentIP();
                        StringBuilder sb = new StringBuilder(64);
                        sb.append(value.substring(0, i)).append(thisip).append(value.substring(i + 10, value.length()));
                        firstcond.setAttribute("value", sb.toString());
                    }
                }
            }
        }
        service.addContent(acls.detach());
    }

    /**
     * @param metadata
     */
    private void checkObjectMetadata(Element metadata) {
        if (metadata.getAttribute("lang") != null) {
            metadata.getAttribute("lang").setNamespace(XML_NAMESPACE);
        }

        List<Element> metadatalist = metadata.getChildren();
        Iterator<Element> metaIt = metadatalist.iterator();

        while (metaIt.hasNext()) {
            Element datatag = metaIt.next();
            if (!checkMetaTags(datatag)) {
                // e.g. datatag is empty
                LOGGER.debug("Removing element :" + datatag.getName());
                metaIt.remove();
            }
        }
    }

    private void checkObjectStructure(Element structure) {
        List<Element> structurelist = structure.getChildren();
        Iterator<Element> structIt = structurelist.iterator();

        while (structIt.hasNext()) {
            Element datatag = structIt.next();
            if (!checkMetaTags(datatag)) {
                // e.g. datatag is empty
                structIt.remove();
            }
        }
    }

    /**
     * The method add a default ACL-block.
     */
    public static void setDefaultDerivateACLs(org.jdom2.Element service) {
        // Read stylesheet and add user
        InputStream aclxml = MCREditorOutValidator.class.getResourceAsStream("/editor_default_acls_derivate.xml");
        if (aclxml == null) {
            LOGGER.warn("Can't find default derivate ACL file editor_default_acls_derivate.xml.");
            return;
        }
        try {
            org.jdom2.Document xml = SAX_BUILDER.build(aclxml);
            org.jdom2.Element acls = xml.getRootElement().getChild("servacls");
            if (acls != null) {
                service.addContent(acls.detach());
            }
        } catch (Exception e) {
            LOGGER.warn("Error while parsing file editor_default_acls_derivate.xml.");
        }
    }

}
