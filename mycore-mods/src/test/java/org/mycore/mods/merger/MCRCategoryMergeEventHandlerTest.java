package org.mycore.mods.merger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.resource.MCRResourceHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
     * Tests if parent genres are successfully removed from mods.
     */
    @Test
    public void testHandleObjectCreatedMultipleGenres() throws IOException, JDOMException, URISyntaxException {

        SAXBuilder saxBuilder = new SAXBuilder();

        MCRCategory category = MCRXMLTransformer
            .getCategory(saxBuilder.build(MCRResourceHelper.getResourceAsStream(TEST_DIRECTORY + "genre.xml")));
        getDAO().addCategory(null, category);

        Document document = saxBuilder.build(MCRResourceHelper.getResourceAsStream(TEST_DIRECTORY + "testMods.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRCategoryMergeEventHandler mergeEventHandler = new MCRCategoryMergeEventHandler();
        mergeEventHandler.handleObjectCreated(null, mcro);
        Document xml = mcro.createXML();

        LOGGER.info(new MCRJDOMContent(xml).asString());

        List<Element> genres = mw.getMODS().getChildren("genre", MCRConstants.MODS_NAMESPACE);
        assertEquals(2, genres.size());
        String url = genres.getFirst().getAttribute("valueURI").getValue();
        String genre = url.substring(url.indexOf('#') + 1);
        assertEquals("book", genre);

        url = genres.get(1).getAttribute("valueURI").getValue();
        genre = url.substring(url.indexOf('#') + 1);
        assertEquals("subchapter", genre);
    }
}
