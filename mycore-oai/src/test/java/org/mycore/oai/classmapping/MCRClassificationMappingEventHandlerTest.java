package org.mycore.oai.classmapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;

import java.io.IOException;
import java.net.URISyntaxException;

public class MCRClassificationMappingEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRClassificationMappingEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.getInstance();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRConfiguration2.set("MCR.Category.XPathMapping.ClassIDs", "orcidWorkType,dummyClassification");
        MCRConfiguration2.set("MCR.Category.XPathMapping.Pattern.genre",
            "//*[@classid='{0}' and @categid='{1}']");
        MCRConfiguration2.set("MCR.Category.XPathMapping.Pattern.host", "//element/publishedin[@type='{0}']");

    }

    /**
     * Tests if x-mappings and XPath-mappings are properly added into a Document.
     * @throws IOException in case of error
     * @throws JDOMException in case of error
     * @throws URISyntaxException in case of error
     */
    @Test
    public void testXMapping() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("genre.xml");
        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject.xml"));
        MCRObject mcro = new MCRObject(document);

        MCRClassificationMappingEventHandler mapper = new MCRClassificationMappingEventHandler();
        mapper.handleObjectUpdated(null, mcro);
        Document xml = mcro.createXML();

        String expression1
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='diniPublType' and @categid='article']";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression1, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for diniPublType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression2
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='schemaOrg' "
                + "and @categid='Article']";
        expressionObject = XPathFactory.instance()
            .compile(expression2, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for schemaOrg should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression3
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='orcidWorkType' "
                + "and @categid='journal-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression3, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for orcidWorkType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression4
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
                + "and @categid='dummy-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression4, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression5
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
                + "and @categid='dummy-placeholder']";
        expressionObject = XPathFactory.instance()
            .compile(expression5, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped placeholder classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(xml));
    }

    /**
     * Tests if the XPath-mappings-fallback mechanism is working correctly and that fallbacks are
     * evaluated per classification.
     * @throws IOException in case of error
     * @throws JDOMException in case of error
     * @throws URISyntaxException in case of error
     */
    @Test
    public void testXPathMappingFallback() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject2.xml"));
        MCRObject mcro = new MCRObject(document);

        MCRClassificationMappingEventHandler mapper = new MCRClassificationMappingEventHandler();
        mapper.handleObjectUpdated(null, mcro);
        Document xml = mcro.createXML();

        String expression1
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='orcidWorkType' "
                + "and @categid='journal-article']";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression1, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for orcidWorkType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression2
            = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
                + "and @categid='dummy-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression2, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(xml));
    }

    private void loadCategory(String categoryFileName) throws URISyntaxException, JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        MCRCategory category = MCRXMLTransformer
            .getCategory(saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + categoryFileName)));
        getDAO().addCategory(null, category);
    }

}
