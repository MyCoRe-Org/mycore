package org.mycore.datamodel.classifications2.utils;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.xml.sax.SAXParseException;

/**
 * This class contains helper methods to handle mycore classifications.
 * 
 * @author Matthias Eichner
 */
public class MCRClassificationUtils {

    public static final String CREATE_CLASS_PERMISSION = "create-class";

    /**
     * Returns the classification as string. Returns null if the
     * classification does not exists.
     * 
     * @param classId the classification root id
     * @return the classification as string
     */
    public static String asString(String classId) {
        Document xml = asDocument(classId);
        return new XMLOutputter(Format.getPrettyFormat()).outputString(xml);
    }

    /**
     * Returns the classification as a jdom document. Returns null if the
     * classification does not exists.
     * 
     * @param classId the classification root id
     * @return the classification as jdom document
     */
    public static Document asDocument(String classId) {
        MCRCategoryID categoryId = MCRCategoryID.rootID(classId);
        MCRCategory classification = MCRCategoryDAOFactory.getInstance().getRootCategory(categoryId, -1);
        if (classification == null) {
            return null;
        }
        return MCRCategoryTransformer.getMetaDataDocument(classification, true);
    }

    /**
     * Imports a classification from the given path. If the classification
     * already exists, it will be replaced.
     * 
     * @param pathToClassification path to the classification
     * @throws IOException could not read from file
     * @throws MCRException xml parsing went wrong
     * @throws SAXParseException xml parsing went wrong
     * @throws URISyntaxException unable to transform the xml to a {@link MCRCategory}
     * @throws MCRAccessException you are not allowed to import the classification
     */
    public static void fromPath(Path pathToClassification)
        throws IOException, MCRException, SAXParseException, MCRAccessException, URISyntaxException {
        InputStream inputStream = Files.newInputStream(pathToClassification);
        fromStream(inputStream);
    }

    /**
     * Imports a classification from the given input stream. If the classification
     * already exists, it will be replaced.
     * 
     * @param inputStream the classification stream
     * @throws MCRException xml parsing went wrong
     * @throws SAXParseException xml parsing went wrong
     * @throws URISyntaxException unable to transform the xml to a {@link MCRCategory}
     * @throws MCRAccessException you are not allowed to import the classification
     */
    public static void fromStream(InputStream inputStream)
        throws MCRException, SAXParseException, URISyntaxException, MCRAccessException {
        MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();
        Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRStreamContent(inputStream));
        MCRCategory classification = MCRXMLTransformer.getCategory(jdom);
        if (DAO.exist(classification.getId())) {
            if (!MCRAccessManager.checkPermission(classification.getId().getRootID(), PERMISSION_WRITE)) {
                throw MCRAccessException.missingPermission(
                    "update classification " + classification.getId().getRootID(), classification.getId().getRootID(),
                    PERMISSION_WRITE);
            }
            DAO.replaceCategory(classification);
        } else {
            if (!MCRAccessManager.checkPermission(CREATE_CLASS_PERMISSION)) {
                throw MCRAccessException.missingPermission(
                    "create classification " + classification.getId().getRootID(), classification.getId().getRootID(),
                    CREATE_CLASS_PERMISSION);
            }
            DAO.addCategory(null, classification);
        }
    }

}
