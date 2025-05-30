/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.metadata.validator;

import static org.jdom2.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;
import static org.mycore.datamodel.metadata.MCRObjectService.ELEMENT_SERVACLS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRMetaAccessRule;
import org.mycore.datamodel.metadata.MCRMetaAddress;
import org.mycore.datamodel.metadata.MCRMetaBoolean;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
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
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.metadata.MCRXMLConstants;
import org.mycore.resource.MCRResourceHelper;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCREditorOutValidator {

    private static final String CONFIG_PREFIX = "MCR.EditorOutValidator.";

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, MCREditorMetadataValidator> VALIDATOR_MAP = getValidatorMap();

    private static final Map<String, Class<? extends MCRMetaInterface>> CLASS_MAP = new HashMap<>();

    private Document input;

    private final MCRObjectID id;

    private final List<String> errorlog;

    /**
     * instantiate the validator with the editor input <code>jdom_in</code>.
     *
     * <code>id</code> will be set as the MCRObjectID for the resulting object
     * that can be fetched with <code>generateValidMyCoReObject()</code>
     *
     * @param jdomIn
     *            editor input
     */
    public MCREditorOutValidator(Document jdomIn, MCRObjectID id) throws JDOMException, IOException {
        errorlog = new ArrayList<>();
        input = jdomIn;
        this.id = id;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XML before validation:\n{}", new XMLOutputter(Format.getPrettyFormat()).outputString(input));
        }
        checkObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XML after validation:\n{}", new XMLOutputter(Format.getPrettyFormat()).outputString(input));
        }
    }

    private static Map<String, MCREditorMetadataValidator> getValidatorMap() {
        Map<String, MCREditorMetadataValidator> map = new HashMap<>();
        map.put(MCRMetaBoolean.class.getSimpleName(), getObjectCheckInstance(MCRMetaBoolean.class));
        map.put(MCRMetaPersonName.class.getSimpleName(), getObjectCheckWithLangInstance(MCRMetaPersonName.class));
        map.put(MCRMetaInstitutionName.class.getSimpleName(),
            getObjectCheckWithLangInstance(MCRMetaInstitutionName.class));
        map.put(MCRMetaAddress.class.getSimpleName(), new MCRMetaAdressCheck());
        map.put(MCRMetaNumber.class.getSimpleName(), getObjectCheckWithLangNotEmptyInstance(MCRMetaNumber.class));
        map.put(MCRMetaLinkID.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaLinkID.class));
        map.put(MCRMetaEnrichedLinkID.class.getSimpleName(),
            getObjectCheckWithLinksInstance(MCRMetaEnrichedLinkID.class));
        map.put(MCRMetaDerivateLink.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaDerivateLink.class));
        map.put(MCRMetaLink.class.getSimpleName(), getObjectCheckWithLinksInstance(MCRMetaLink.class));
        map.put(MCRMetaISO8601Date.class.getSimpleName(),
            getObjectCheckWithLangNotEmptyInstance(MCRMetaISO8601Date.class));
        map.put(MCRMetaLangText.class.getSimpleName(), getObjectCheckWithLangNotEmptyInstance(MCRMetaLangText.class));
        map.put(MCRMetaAccessRule.class.getSimpleName(), getObjectCheckInstance(MCRMetaAccessRule.class));
        map.put(MCRMetaClassification.class.getSimpleName(), new MCRMetaClassificationCheck());
        map.put(MCRMetaHistoryDate.class.getSimpleName(), new MCRMetaHistoryDateCheck());
        Map<String, String> props = MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(CONFIG_PREFIX + "class."))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        for (Entry<String, String> entry : props.entrySet()) {
            try {
                String className = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
                LOGGER.info("Adding Validator {} for class {}", entry::getValue, () -> className);
                @SuppressWarnings("unchecked")
                Class<? extends MCREditorMetadataValidator> cl = (Class<? extends MCREditorMetadataValidator>) Class
                    .forName(entry.getValue());
                map.put(className, cl.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                final String msg = "Cannot instantiate " + entry.getValue() + " as validator for class "
                    + entry.getKey();
                LOGGER.error(msg);
                throw new MCRException(msg, e);
            }
        }
        return map;
    }

    public static Class<? extends MCRMetaInterface> getClass(String mcrclass) throws ClassNotFoundException {
        Class<? extends MCRMetaInterface> clazz = CLASS_MAP.get(mcrclass);
        if (clazz == null) {
            clazz = MCRClassTools.forName("org.mycore.datamodel.metadata." + mcrclass);
            CLASS_MAP.put(mcrclass, clazz);
        }
        return clazz;
    }

    public static String checkMetaObject(Element datasubtag, Class<? extends MCRMetaInterface> metaClass,
        boolean keepLang) {
        if (!keepLang) {
            datasubtag.removeAttribute(MCRXMLConstants.LANG, XML_NAMESPACE);
        }
        try {
            MCRMetaInterface test = metaClass.getDeclaredConstructor().newInstance();
            test.setFromDOM(datasubtag);
            test.validate();
        } catch (Exception e) {
            throw new MCRException("Could not instantiate " + metaClass.getCanonicalName(), e);
        }
        return null;
    }

    public static String checkMetaObjectWithLang(Element datasubtag, Class<? extends MCRMetaInterface> metaClass) {
        if (datasubtag.getAttribute(MCRXMLConstants.LANG) != null) {
            datasubtag.getAttribute(MCRXMLConstants.LANG).setNamespace(XML_NAMESPACE);
            LOGGER.warn("namespace add for xml:lang attribute in {}", datasubtag::getName);
        }
        return checkMetaObject(datasubtag, metaClass, true);
    }

    public static String checkMetaObjectWithLangNotEmpty(Element datasubtag,
        Class<? extends MCRMetaInterface> metaClass) {
        String text = datasubtag.getTextTrim();
        if (text == null || text.isEmpty()) {
            return "Element " + datasubtag.getName() + " has no text.";
        }
        return checkMetaObjectWithLang(datasubtag, metaClass);
    }

    public static String checkMetaObjectWithLinks(Element datasubtag, Class<? extends MCRMetaInterface> metaClass) {
        if (datasubtag.getAttributeValue(MCRXMLConstants.HREF) == null
            && datasubtag.getAttributeValue(MCRXlink.HREF, XLINK_NAMESPACE) == null) {
            return datasubtag.getName() + " has no href attribute defined";
        }
        if (datasubtag.getAttribute(MCRXMLConstants.X_TYPE) != null) {
            datasubtag.getAttribute(MCRXMLConstants.X_TYPE).setNamespace(XLINK_NAMESPACE).setName(MCRXlink.TYPE);
        } else if (datasubtag.getAttribute(MCRXMLConstants.TYPE) != null
            && datasubtag.getAttribute(MCRXlink.TYPE, XLINK_NAMESPACE) == null) {
            datasubtag.getAttribute(MCRXMLConstants.TYPE).setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:type attribute in {}", datasubtag::getName);
        }
        if (datasubtag.getAttribute(MCRXMLConstants.HREF) != null) {
            datasubtag.getAttribute(MCRXMLConstants.HREF).setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:href attribute in {}", datasubtag::getName);
        }

        if (datasubtag.getAttribute(MCRXMLConstants.TITLE) != null) {
            datasubtag.getAttribute(MCRXMLConstants.TITLE).setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:title attribute in {}", datasubtag::getName);
        }
        if (datasubtag.getAttribute(MCRXMLConstants.LABEL) != null) {
            datasubtag.getAttribute(MCRXMLConstants.LABEL).setNamespace(XLINK_NAMESPACE);
            LOGGER.warn("namespace add for xlink:label attribute in {}", datasubtag::getName);
        }
        return checkMetaObject(datasubtag, metaClass, false);
    }

    static MCREditorMetadataValidator getObjectCheckInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> checkMetaObject(datasubtag, clazz, false);
    }

    static MCREditorMetadataValidator getObjectCheckWithLangInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> checkMetaObjectWithLang(datasubtag, clazz);
    }

    static MCREditorMetadataValidator getObjectCheckWithLangNotEmptyInstance(
        final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> checkMetaObjectWithLangNotEmpty(datasubtag, clazz);
    }

    static MCREditorMetadataValidator getObjectCheckWithLinksInstance(final Class<? extends MCRMetaInterface> clazz) {
        return datasubtag -> checkMetaObjectWithLinks(datasubtag, clazz);
    }

    /**
     * The method add a default ACL-block.
     */
    public static void setDefaultDerivateACLs(Element service) {
        // Read stylesheet and add user
        InputStream aclxml = MCRResourceHelper.getResourceAsStream("editor_default_acls_derivate.xml");
        if (aclxml == null) {
            LOGGER.warn("Can't find default derivate ACL file editor_default_acls_derivate.xml.");
            return;
        }
        try {
            Document xml = SAX_BUILDER.build(aclxml);
            Element acls = xml.getRootElement().getChild(ELEMENT_SERVACLS);
            if (acls != null) {
                service.addContent(acls.detach());
            }
        } catch (Exception e) {
            LOGGER.warn("Error while parsing file editor_default_acls_derivate.xml.");
        }
    }

    /**
     * tries to generate a valid MCRObject as JDOM Document.
     *
     * @return MCRObject
     */
    public Document generateValidMyCoReObject() throws JDOMException, IOException {
        MCRObject obj;
        // load the JDOM object
        XPathFactory.instance()
            .compile("/mycoreobject/*/*/*/@editor.output", Filters.attribute())
            .evaluate(input)
            .forEach(Attribute::detach);
        try {
            byte[] xml = new MCRJDOMContent(input).asByteArray();
            obj = new MCRObject(xml, true);
        } catch (JDOMException e) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            LOGGER.warn("Failure while parsing document:\n{}", () -> xout.outputString(input));
            throw e;
        }
        // remove that, because its set in MCRMetadataManager, and we need the information (MCR-2603).
        //Date curTime = new Date();
        //obj.getService().setDate("modifydate", curTime);

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

    private void checkObject() throws JDOMException, IOException {
        // add the namespaces (this is a workaround)
        Element root = input.getRootElement();
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        // set the schema
        String mcrSchema = "datamodel-" + id.getTypeId() + ".xsd";
        root.setAttribute("noNamespaceSchemaLocation", mcrSchema, XSI_NAMESPACE);
        // check the label
        String label = MCRUtils.filterTrimmedNotEmpty(root.getAttributeValue(MCRXMLConstants.LABEL))
            .orElse(null);
        if (label == null) {
            root.setAttribute(MCRXMLConstants.LABEL, id.toString());
        }
        // remove the path elements from the incoming
        Element pathes = root.getChild("pathes");
        if (pathes != null) {
            root.removeChildren("pathes");
        }
        Element structure = root.getChild(MCRObjectStructure.XML_NAME);
        if (structure == null) {
            root.addContent(new Element(MCRObjectStructure.XML_NAME));
        } else {
            checkObjectStructure(structure);
        }
        Element metadata = root.getChild(MCRObjectMetadata.XML_NAME);
        checkObjectMetadata(metadata);
        Element service = root.getChild(MCRObjectService.XML_NAME);
        checkObjectService(root, service);
    }

    @SuppressWarnings("PMD.UnusedAssignment")
    private boolean checkMetaTags(Element datatag) {
        String mcrclass = datatag.getAttributeValue(MCRXMLConstants.CLASS);
        List<Element> datataglist = datatag.getChildren();
        Iterator<Element> datatagIt = datataglist.iterator();

        while (datatagIt.hasNext()) {
            Element datasubtag = datatagIt.next();
            MCREditorMetadataValidator validator = VALIDATOR_MAP.get(mcrclass);
            String returns = null;
            if (validator != null) {
                returns = validator.checkDataSubTag(datasubtag);
            } else {
                LOGGER.warn("Tag <{}> of type {} has no validator defined, fallback to default behaviour",
                    datatag::getName, () -> mcrclass);
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
        return !datatag.getChildren().isEmpty();
    }

    private void checkObjectService(Element root, Element service) throws JDOMException, IOException {
        Element validatedService;
        if (service == null) {
            validatedService = new Element(MCRObjectService.XML_NAME);
            root.addContent(validatedService);
        } else {
            validatedService = service;
        }
        List<Element> servicelist = validatedService.getChildren();
        for (Element datatag : servicelist) {
            checkMetaTags(datatag);
        }

        if (validatedService.getChild(ELEMENT_SERVACLS) == null &&
            MCRAccessManager.getAccessImpl() instanceof MCRRuleAccessInterface) {
            Collection<String> li = MCRAccessManager.getPermissionsForID(id.toString());
            if (li == null || li.isEmpty()) {
                setDefaultObjectACLs(validatedService);
            }
        }
    }

    /**
     * The method add a default ACL-block.
     *
     */
    private void setDefaultObjectACLs(Element service) throws JDOMException, IOException {
        if (!MCRConfiguration2.getBoolean("MCR.Access.AddObjectDefaultRule").orElse(true)) {
            LOGGER.info("Adding object default acl rule is disabled.");
            return;
        }
        Element acls = loadDefaultAclDocument().getRootElement().getChild(ELEMENT_SERVACLS);
        if (acls == null) {
            return;
        }
        for (Element acl : acls.getChildren()) {
            Element condition = acl.getChild("condition");
            if (condition == null) {
                continue;
            }
            Element rootbool = condition.getChild("boolean");
            if (rootbool == null) {
                continue;
            }
            updateConditionsValueAttribute(rootbool);
        }
        service.addContent(acls.detach());
    }

    private void updateConditionsValueAttribute(Element rootbool) {
        for (Element orbool : rootbool.getChildren("boolean")) {
            for (Element firstcond : orbool.getChildren("condition")) {
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
                        "The parameter $CurrentGroup in default ACLs is not supported as of MyCoRe 2014.06"
                            + " because it is not supported in Servlet API 3.0");
                }
                int i = value.indexOf("$CurrentIP");
                if (i != -1) {
                    String thisip = MCRSessionMgr.getCurrentSession().getCurrentIP();
                    firstcond.setAttribute("value",
                        value.substring(0, i) + thisip + value.substring(i + 10));
                }
            }
        }
    }

    private Document loadDefaultAclDocument() throws IOException, JDOMException {
        String resourcetype = "editor_default_acls_" + id.getTypeId() + ".xml";
        String resourcebase = "editor_default_acls_" + id.getBase() + ".xml";
        InputStream aclxml = MCRResourceHelper.getResourceAsStream(resourcebase);
        if (aclxml == null) {
            aclxml = MCRResourceHelper.getResourceAsStream(resourcetype);
            if (aclxml == null) {
                LOGGER.warn("Can't find default object ACL file {} or {}",
                    resourcebase, resourcetype);
                String resource = "editor_default_acls.xml"; // fallback
                aclxml = MCRResourceHelper.getResourceAsStream(resource);
                if (aclxml == null) {
                    return null;
                }
            }
        }
        return SAX_BUILDER.build(aclxml);
    }

    private void checkObjectMetadata(Element metadata) {
        if (metadata.getAttribute(MCRXMLConstants.LANG) != null) {
            metadata.getAttribute(MCRXMLConstants.LANG).setNamespace(XML_NAMESPACE);
        }

        List<Element> metadatalist = metadata.getChildren();
        Iterator<Element> metaIt = metadatalist.iterator();

        while (metaIt.hasNext()) {
            Element datatag = metaIt.next();
            if (!checkMetaTags(datatag)) {
                // e.g. datatag is empty
                LOGGER.debug("Removing element :{}", datatag::getName);
                metaIt.remove();
            }
        }
    }

    private void checkObjectStructure(Element structure) {
        // e.g. datatag is empty
        structure.getChildren().removeIf(datatag -> !checkMetaTags(datatag));
    }

    static class MCRMetaHistoryDateCheck implements MCREditorMetadataValidator {
        @Override
        public String checkDataSubTag(Element datasubtag) {
            Element[] children = datasubtag.getChildren("text").toArray(Element[]::new);
            int textCount = children.length;
            for (Element child : children) {
                String text = child.getTextTrim();
                if (text == null || text.isEmpty()) {
                    child.detach();
                    textCount--;
                    continue;
                }
                if (child.getAttribute(MCRXMLConstants.LANG) != null) {
                    child.getAttribute(MCRXMLConstants.LANG).setNamespace(XML_NAMESPACE);
                    LOGGER.warn("namespace add for xml:lang attribute in {}", datasubtag::getName);
                }
            }
            if (textCount == 0) {
                return "history date is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaHistoryDate.class);
        }
    }

    static class MCRMetaClassificationCheck implements MCREditorMetadataValidator {
        @Override
        public String checkDataSubTag(Element datasubtag) {
            String categid = datasubtag.getAttributeValue("categid");
            if (categid == null) {
                return "Attribute categid is empty";
            }
            return checkMetaObject(datasubtag, MCRMetaClassification.class, false);
        }
    }

    static class MCRMetaAdressCheck implements MCREditorMetadataValidator {
        @Override
        public String checkDataSubTag(Element datasubtag) {
            if (datasubtag.getChildren().isEmpty()) {
                return "adress is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaAddress.class);
        }
    }

    static class MCRMetaPersonNameCheck implements MCREditorMetadataValidator {
        @Override
        public String checkDataSubTag(Element datasubtag) {
            if (datasubtag.getChildren().isEmpty()) {
                return "person name is empty";
            }
            return checkMetaObjectWithLang(datasubtag, MCRMetaAddress.class);
        }
    }

}
