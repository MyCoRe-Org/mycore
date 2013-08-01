package org.mycore.migration21_22.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.migration21_22.user.MCRGroup;
import org.mycore.migration21_22.user.MCRUser;
import org.mycore.migration21_22.user.MCRUserContact;
import org.mycore.migration21_22.user.hibernate.MCRHIBUserStore;
import org.mycore.user2.MCRPasswordHashType;
import org.mycore.user2.MCRRole;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUserManager;
import org.xml.sax.SAXException;

@MCRCommandGroup(name = "MCR Migration Commands from MyCoRe version 2.1 to 2.2")
public class MCRMigrationCommands22 extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands22.class);

    public MCRMigrationCommands22() {
    }

    @MCRCommand(help = "Move xlink label which is not NCName to xlink:title.", syntax = "migrate xlink label")
    public static void xlinkLabelMigration() throws TransformerException, JDOMException, IOException, SAXException {
        MCRXMLMetadataManager xmlMetaManager = MCRXMLMetadataManager.instance();
        List<String> listIDs = xmlMetaManager.listIDs();

        InputStream resourceAsStream = MCRMigrationCommands22.class.getResourceAsStream("/xsl/xlinkLabelMigration.xsl");
        Source stylesheet = new StreamSource(resourceAsStream);
        Transformer xsltTransformer = MCRXSLTransformation.getInstance().getStylesheet(stylesheet).newTransformer();

        XPathExpression<Attribute> xlinkLabel = XPathFactory.instance().compile("/mycoreobject/*/*[starts-with(@class,'MCRMetaLink')]/*/@xlink:label",
            Filters.attribute(), null, MCRConstants.XLINK_NAMESPACE);
        for (String ID : listIDs) {
            MCRObjectID mcrid = MCRObjectID.getInstance(ID);
            Document mcrObjXML = xmlMetaManager.retrieveXML(mcrid);
            if (xlinkLabel.evaluateFirst(mcrObjXML) != null) {
                Source xmlSource = new JDOMSource(mcrObjXML);
                JDOMResult jdomResult = new JDOMResult();
                xsltTransformer.transform(xmlSource, jdomResult);
                Document migratedMcrObjXML = jdomResult.getDocument();

                xmlMetaManager.update(mcrid, migratedMcrObjXML, new Date());
                LOGGER.info("Migrated xlink for " + mcrid);
            } else {
                LOGGER.info("No xlink migration for " + mcrid);
            }
        }
    }

    @MCRCommand(help = "Replace ':' in categID with '_'", syntax = "fix colon in categID")
    public static void fixCategID() throws JDOMException, TransformerException, IOException, SAXException {
        Session dbSession = MCRHIBConnection.instance().getSession();
        dbSession.createSQLQuery("update MCRCATEGORY set CATEGID=replace(categid,':','-') where CATEGID like '%:%'").executeUpdate();

        MCRXMLMetadataManager xmlMetaManager = MCRXMLMetadataManager.instance();
        List<String> listIDs = xmlMetaManager.listIDs();

        InputStream resourceAsStream = MCRMigrationCommands22.class.getResourceAsStream("/xsl/replaceColoneInCategID.xsl");
        Source stylesheet = new StreamSource(resourceAsStream);
        Transformer xsltTransformer = MCRXSLTransformation.getInstance().getStylesheet(stylesheet).newTransformer();

        XPathExpression<Element> xlinkLabel = XPathFactory.instance().compile(
            "/mycoreobject/metadata/*[@class='MCRMetaClassification']/*[contains(@categid,':')]", Filters.element());
        for (String ID : listIDs) {
            MCRObjectID mcrid = MCRObjectID.getInstance(ID);
            Document mcrObjXML = xmlMetaManager.retrieveXML(mcrid);
            if (xlinkLabel.evaluateFirst(mcrObjXML) != null) {
                Source xmlSource = new JDOMSource(mcrObjXML);
                JDOMResult jdomResult = new JDOMResult();
                xsltTransformer.transform(xmlSource, jdomResult);
                Document migratedMcrObjXML = jdomResult.getDocument();

                xmlMetaManager.update(mcrid, migratedMcrObjXML, new Date());
                LOGGER.info("Replace ':' in categID for " + mcrid);
            } else {
                LOGGER.info("Nothing to replace for " + mcrid);
            }
        }
    }

    @MCRCommand(syntax = "migrate users", help = "Migrate user from MyCoRe < 2.2")
    public static void migrateUsers() {
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        final String suser = CONFIG.getString("MCR.Users.Superuser.UserName", "administrator");
        //convert groups
        List<String> groupIDs = userStore.getAllGroupIDs();

        for (String groupID : groupIDs) {
            MCRRole role = MCRRoleManager.getRole(groupID);
            if (role != null) {
                LOGGER.warn("Role does already exist: " + role.getName());
                continue;
            }
            LOGGER.info("Adding group: " + groupID);
            MCRGroup group = userStore.retrieveGroup(groupID);
            createRole(group);
        }

        List<String> userIDs = userStore.getAllUserIDs();
        org.mycore.user2.MCRUser superUser = MCRUserManager.getUser(suser);
        if (superUser == null) {
            //initialize new user system
            LOGGER.info("Adding super user: " + suser);
            MCRUser user = userStore.retrieveUser(suser);
            createUser(user, null);
            superUser = MCRUserManager.getUser(suser);
        }

        for (String userID : userIDs) {
            if (userID.equals(MCRSystemUserInformation.getGuestInstance().getUserID())) {
                LOGGER.warn("Guest user will not be migrated: " + userID);
                continue;
            }
            if (userID.equals(superUser.getUserID()) || MCRUserManager.exists(userID)) {
                LOGGER.warn("User does already exist: " + userID);
                continue;
            }
            LOGGER.info("Adding user: " + userID);
            MCRUser user = userStore.retrieveUser(userID);
            createUser(user, superUser);
        }
    }

    private static void createRole(MCRGroup group) {
        HashSet<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel(MCRConstants.DEFAULT_LANG, group.getID(), group.getDescription()));
        MCRRole role = new MCRRole(group.getID(), labels);
        MCRRoleManager.addRole(role);
    }

    private static void createUser(MCRUser user, org.mycore.user2.MCRUser superUser) {
        org.mycore.user2.MCRUser newUser = new org.mycore.user2.MCRUser(user.getID());
        newUser.setEMail(user.getUserContact().getEmail());
        newUser.setHashType(MCRPasswordHashType.crypt);
        newUser.setPassword(user.getPassword());
        newUser.setRealName(getRealName(user));
        if (!user.isEnabled()) {
            newUser.setValidUntil(new Date());
        }
        newUser.setLocked(!user.isUpdateAllowed());
        if (newUser.isLocked() && superUser != null) {
            newUser.setOwner(superUser);
        }
        newUser.assignRole(user.getPrimaryGroupID());
        for (String group : user.getGroupIDs()) {
            newUser.assignRole(group);
        }
        newUser.setAttributes(getUserAttributes(user));
        MCRUserManager.createUser(newUser);
    }

    private static String getRealName(MCRUser user) {
        String firstName = user.getUserContact().getFirstName();
        String lastName = user.getUserContact().getLastName();
        StringBuilder realName = new StringBuilder();
        if (firstName != null && firstName.length() > 0) {
            realName.append(firstName);
        }
        if (lastName != null && lastName.length() > 0) {
            if (realName.length() > 0) {
                realName.append(' ');
            }
            realName.append(lastName);
        }
        return realName.toString();
    }

    private static Map<String, String> getUserAttributes(MCRUser user) {
        Map<String, String> att = new TreeMap<String, String>();
        MCRUserContact contact = user.getUserContact();
        setAttribute(att, "user.creator", user.getCreator());
        if (user.getCreationDate() != null) {
            MCRISO8601Date date = new MCRISO8601Date();
            date.setDate(user.getCreationDate());
            date.setFormat(MCRISO8601Format.COMPLETE_HH_MM_SS);
            att.put("user.creation_date", date.getISOString());
        }
        if (user.getModifiedDate() != null) {
            MCRISO8601Date date = new MCRISO8601Date();
            date.setDate(user.getModifiedDate());
            date.setFormat(MCRISO8601Format.COMPLETE_HH_MM_SS);
            att.put("user.last_modified", date.getISOString());
        }
        setAttribute(att, "user.description", user.getDescription());
        att.put("user.primary_group", user.getPrimaryGroupID());
        setAttribute(att, "contact.salutation", contact.getSalutation());
        setAttribute(att, "contact.street", contact.getStreet());
        setAttribute(att, "contact.city", contact.getCity());
        setAttribute(att, "contact.postalcode", contact.getPostalCode());
        setAttribute(att, "contact.country", contact.getCountry());
        setAttribute(att, "contact.state", contact.getState());
        setAttribute(att, "contact.institution", contact.getInstitution());
        setAttribute(att, "contact.faculty", contact.getFaculty());
        setAttribute(att, "contact.department", contact.getDepartment());
        setAttribute(att, "contact.institute", contact.getInstitute());
        setAttribute(att, "contact.telephone", contact.getTelephone());
        setAttribute(att, "contact.fax", contact.getFax());
        setAttribute(att, "contact.cellphone", contact.getCellphone());
        return att;
    }

    private static void setAttribute(Map<String, String> att, String attrName, String value) {
        if (value != null && value.length() > 0) {
            att.put(attrName, value);
        }
    }
}
