package org.mycore.migration21_22.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

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
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

@MCRCommandGroup(name = "Migrate from 2.1 to 2.2")
public class MCRMigrationCommands22 extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands22.class);

    public MCRMigrationCommands22() {
    }

    @MCRCommand(help = "Move xlink label which is not NCName to xlink:title.", syntax = "migrate xlink label")
    public static void xlinkLabelMigration() throws TransformerException, JDOMException, IOException {
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

    @MCRCommand(help = "Replace ':' in categID with '_'", syntax = "fix colone in categID")
    public static void fixCategID() throws JDOMException, TransformerException {
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
}
