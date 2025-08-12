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

package org.mycore.mods.classification;

import java.io.IOException;
import java.net.URISyntaxException;

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
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

public class MCRMODSClassificationMapperTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = MCRMODSClassificationMapperTest.class.getSimpleName() + "/";

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.obtainInstance();
    }

    @Test
    public void testMapping() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("diniPublType.xml");
        loadCategory("genre.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        String expression =
            "//mods:classification[contains(@generator,'-mycore') and contains(@valueURI, 'StudyThesis')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
        Document xml = mcro.createXML();
        Assert.assertNotNull("The mapped classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        expression = "//mods:classification[contains(@generator,'-mycore') and contains(@valueURI, 'masterThesis')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNull("The mapped classification of the child should not be contained in the MyCoReObject now!",
            expressionObject.evaluateFirst(xml));

    }

    /**
     * Tests if an XPath-mapping is properly added into a Mods-Document. Also tests, if multiple fallbacks per
     * classification are considered
     * @throws IOException in case of error
     * @throws JDOMException in case of error
     * @throws URISyntaxException in case of error
     */
    @Test
    public void testXPathMapping() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("genre.xml");
        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods2.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 2);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        Document xml = mcro.createXML();

        String expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2orcidWorkType') and contains(@valueURI, 'journal-article')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2dummyClassification') and contains(@valueURI, 'dummy-text')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(xml));

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2dummyClassification') and contains(@valueURI, 'dummy-fbonly')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(xml));

    }

    /**
     * Tests if a fallback XPath-mapping is properly added into a Mods-Document if the original mapping doesn't match.
     * @throws IOException in case of error
     * @throws JDOMException in case of error
     * @throws URISyntaxException in case of error
     */
    @Test
    public void testXPathMappingFallback() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("genre.xml");
        loadCategory("orcidWorkType.xml");
        loadCategory("dummyClassification.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods3.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 3);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        Document xml = mcro.createXML();

        String expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2orcidWorkType') and contains(@valueURI, 'online-resource')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2dummyClassification') and contains(@valueURI, 'dummy-text')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(xml));

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2dummyClassification') and contains(@valueURI, 'dummy-fbonly')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped dummy classification should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(xml));

    }

    /**
     * Tests if placeholder patterns are properly evaluated in different scenarios. The tested cases are:<p>
     * <ol>
     *     <li>A pattern with 3 values to substitute</li>
     *     <li>A pattern that tests for a specific genre</li>
     *     <li>A pattern without values to substitute</li>
     *     <li>Multiple patterns in one XPath</li>
     * </ol>
     */
    @Test
    public void testXPathMappingPlaceholders() throws URISyntaxException, IOException, JDOMException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("placeholderClassification.xml");

        // test placeholder with multiple values
        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 4);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        Document xml = mcro.createXML();

        String expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderClassification') "
            + "and contains(@valueURI, 'dummy-placeholder-language')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull(
            "The mapped classification 'dummy-placeholder-language' should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        // test placeholder with genre
        document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods2.xml"));
        mcro = new MCRObject();
        mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 5);

        mapper.createMapping(mcro);
        xml = mcro.createXML();

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderClassification') "
            + "and contains(@valueURI, 'dummy-placeholder-genre')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped classification 'dummy-placeholder-genre' should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        // test fallback placeholder without values
        document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods3.xml"));
        mcro = new MCRObject();
        mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 6);

        mapper.createMapping(mcro);
        xml = mcro.createXML();

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderClassification') "
            + "and contains(@valueURI, 'dummy-placeholder-fb')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped classification 'dummy-placeholder-fb' should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        // test multiple placeholders in one XPath
        document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods2.xml"));
        mcro = new MCRObject();
        mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 7);

        mapper.createMapping(mcro);
        xml = mcro.createXML();

        expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderClassification') "
            + "and contains(@valueURI, 'dummy-placeholder-multiple')]";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull("The mapped classification 'dummy-placeholder-multiple' "
            + "should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

    }


    /**
     * Tests if placeholder patterns containing OR-conditions are properly evaluated.
     */
    @Test
    public void testXPathMappingPlaceholdersOROperator() throws URISyntaxException, IOException, JDOMException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        loadCategory("placeholderOrCondition.xml");

        // test placeholder with or-condition
        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods2.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 8);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        Document xml = mcro.createXML();

        String expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderOrCondition') "
            + "and contains(@valueURI, 'dummy-placeholder-or-condition')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull(
            "The mapped classification 'dummy-placeholder-or-condition' should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));
    }

    /**
     * Tests if placeholder patterns containing OR-conditions are properly evaluated in the fallback mechanism.
     */
    @Test
    public void testXPathMappingFallbackAndORCondition() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        loadCategory("placeholderOrCondition.xml");

        // test fallback with a placeholder with or-condition
        Document document1 = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods3.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document1.getRootElement().detach());
        mw.setID("junit", 9);

        MCRMODSClassificationMapper mapper = new MCRMODSClassificationMapper();
        mapper.createMapping(mcro);

        Document xml = mcro.createXML();

        String expression = "//mods:classification[contains(@generator,'-mycore') and "
            + "contains(@generator,'xpathmapping2placeholderOrCondition') "
            + "and contains(@valueURI, 'dummy-placeholder-or-condition-fb')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNotNull(
            "The mapped classification 'dummy-placeholder-or-condition-fb' should be in the MyCoReObject now!",
            expressionObject.evaluateFirst(
                xml));

        // test fallback with a placeholder with or-condition
        Document document2 = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMods4.xml"));
        mcro = new MCRObject();

        mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document2.getRootElement().detach());
        mw.setID("junit", 10);

        mapper.createMapping(mcro);

        xml = mcro.createXML();

        expression = "//mods:classification";
        expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);

        Assert.assertNull(
            "The mapped classification 'dummy-placeholder-or-condition-fb' should NOT be in the MyCoReObject now!",
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

}
