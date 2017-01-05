package org.mycore.mods.classification;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.xml.sax.SAXParseException;


public class MCRClassificationMappingEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRClassificationMappingEventHandlerTest/";
    private static final Logger LOGGER = Logger.getLogger(MCRClassificationMappingEventHandlerTest.class);

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.getInstance();
    }

    @Test
    public void testMapping() throws SAXParseException, IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession().isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("diniPublType.xml");
        loadCategory("genre.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRClassificationMappingEventHandler mapper = new MCRClassificationMappingEventHandler();
        mapper.handleObjectUpdated(null, mcro);

        String expression = "//mods:classification[contains(@generator,'-mycore') and contains(@valueURI, 'StudyThesis')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance().compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification should be in the MyCoReObject now!", expressionObject.evaluateFirst(mcro.createXML()));
    }

    private void loadCategory(String categoryFileName) throws URISyntaxException, JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        MCRCategory category = MCRXMLTransformer.getCategory(saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + categoryFileName)));
        getDAO().addCategory(null, category);
    }

}