package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

import java.io.IOException;
import java.net.URISyntaxException;

public class MCRCategoryMergeEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRCategoryMergeEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.getInstance();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test has no assertions yet, only logs final mods.
     * TODO: Current implementation changes order of genres, fix?
     */
    @Test
    public void testHandleObjectCreatedMultipleGenres() throws IOException, JDOMException, URISyntaxException {

        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        MCRCategory category = MCRXMLTransformer
            .getCategory(saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "genre.xml")));
        getDAO().addCategory(null, category);

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRCategoryMergeEventHandler mergeEventHandler = new MCRCategoryMergeEventHandler();
        mergeEventHandler.handleObjectCreated(null, mcro);
        Document xml = mcro.createXML();

        LOGGER.info(new MCRJDOMContent(xml).asString());
    }
}
