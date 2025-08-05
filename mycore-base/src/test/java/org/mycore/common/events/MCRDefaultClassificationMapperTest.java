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

package org.mycore.common.events;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

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
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRDefaultClassificationMapper;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRDefaultClassificationMapperTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRClassificationMappingEventHandlerTest/";

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.obtainInstance();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Tests if x-mappings and XPath-mappings are properly added into a Document.
     */
    @Test
    public void testXMapping() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("genre.xml");
        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject.xml"));
        MCRObject mcro = new MCRObject(document);

        MCRDefaultClassificationMapper mapper = new MCRDefaultClassificationMapper();
        mapper.clearMappings(mcro);
        mapper.createMapping(mcro);
        Document xml = mcro.createXML();

        String expression1 =
            "//mappings[@class='MCRMetaClassification']/mapping[@classid='diniPublType' and @categid='article']";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression1, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for diniPublType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression2 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='schemaOrg' "
            + "and @categid='Article']";
        expressionObject = XPathFactory.instance()
            .compile(expression2, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for schemaOrg should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression3 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='orcidWorkType' "
            + "and @categid='journal-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression3, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for orcidWorkType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression4 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
            + "and @categid='dummy-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression4, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression5 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
            + "and @categid='dummy-placeholder']";
        expressionObject = XPathFactory.instance()
            .compile(expression5, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped placeholder classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));
    }

    /**
     * Tests if the XPath-mappings-fallback mechanism is working correctly and that fallbacks are
     * evaluated per classification.
     */
    @Test
    public void testXPathMappingFallback() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject2.xml"));
        MCRObject mcro = new MCRObject(document);

        MCRDefaultClassificationMapper mapper = new MCRDefaultClassificationMapper();
        mapper.clearMappings(mcro);
        mapper.createMapping(mcro);
        Document xml = mcro.createXML();

        String expression1 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='orcidWorkType' "
            + "and @categid='journal-article']";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression1, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped classification for orcidWorkType should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        String expression2 = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
            + "and @categid='dummy-article']";
        expressionObject = XPathFactory.instance()
            .compile(expression2, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

    }

    /**
     * Tests that XPath-mappings with OR-condition are properly added into a Document.
     */
    @Test
    public void testXPathMappingPatternORCondition() throws IOException, JDOMException, URISyntaxException {

        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject3.xml"));
        MCRObject mcro = new MCRObject(document);

        MCRDefaultClassificationMapper mapper = new MCRDefaultClassificationMapper();
        mapper.createMapping(mcro);
        Document xml = mcro.createXML();

        String expression = "//mappings[@class='MCRMetaClassification']/mapping[@classid='dummyClassification' "
            + "and @categid='dummy-or-condition']";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.XLINK_NAMESPACE);
        Assert.assertNotNull("The mapped or-condition dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));
    }

    private void loadCategory(String categoryFileName) throws URISyntaxException, JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        MCRCategory category = MCRXMLTransformer
            .getCategory(saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + categoryFileName)));
        getDAO().addCategory(null, category);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Category.XPathMapping.ClassIDs", "orcidWorkType,dummyClassification");
        testProperties.put("MCR.Category.XPathMapping.Pattern.genre", "//*[@classid='{0}' and @categid='{1}']");
        testProperties.put("MCR.Category.XPathMapping.Pattern.host", "//element/publishedin[@type='{0}']");
        return testProperties;
    }

}
