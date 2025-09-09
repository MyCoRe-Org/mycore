/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.mapping.MCRConditionalXMappingEvaluator;
import org.mycore.datamodel.classifications2.mapping.MCRXMappingClassificationGeneratorBase.OnMissingMappedCategory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.mapping.MCRMODSGeneratorClassificationMapper;
import org.mycore.mods.classification.mapping.MCRMODSParentGenreClassificationMappingCondition;
import org.mycore.mods.classification.mapping.MCRMODSXMappingClassificationGenerator;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRConditionalXMappingEvaluatorTest {

    public static final String TEST_DIRECTORY = MCRConditionalXMappingEvaluatorTest.class.getSimpleName() + "/";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.obtainInstance();
    }

    @Test
    public void testMappingJournal() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("diniPublType.xml");
        loadCategory("genre.xml");

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testModsJournal.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRMODSGeneratorClassificationMapper mapper = new MCRMODSGeneratorClassificationMapper(
            Map.of(
                "xMapping",
                new MCRMODSXMappingClassificationGenerator(
                    new MCRConditionalXMappingEvaluator(
                        Map.of(
                            "parentGenre",
                            new MCRMODSParentGenreClassificationMappingCondition())),
                    OnMissingMappedCategory.IGNORE)));
        mapper.createMappings(mcro);

        Document xml = mcro.createXML();
        LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(xml));

        String expression = "//mods:classification[contains(@generator,'-mycore') and contains(@valueURI, 'article')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
        assertNotNull(expressionObject.evaluateFirst(
            xml), "The mapped classification should be in the MyCoReObject now!");

    }

    @Test
    public void testMappingCollection() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        loadCategory("diniPublType.xml");
        loadCategory("genre.xml");

        Document document =
            saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testModsCollection.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRMODSGeneratorClassificationMapper mapper = new MCRMODSGeneratorClassificationMapper(
            Map.of(
                "xMapping",
                new MCRMODSXMappingClassificationGenerator(
                    new MCRConditionalXMappingEvaluator(
                        Map.of(
                            "parentGenre",
                            new MCRMODSParentGenreClassificationMappingCondition())),
                    OnMissingMappedCategory.IGNORE)));
        mapper.createMappings(mcro);

        Document xml = mcro.createXML();
        LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(xml));

        String expression = "//mods:classification[contains(@generator,'-mycore') and contains(@valueURI, 'bookPart')]";
        XPathExpression<Element> expressionObject = XPathFactory.instance()
            .compile(expression, Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE);
        assertNotNull(expressionObject.evaluateFirst(
            xml), "The mapped classification should be in the MyCoReObject now!");

    }

    private void loadCategory(String categoryFileName) throws URISyntaxException, JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        MCRCategory category = MCRXMLTransformer
            .getCategory(saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + categoryFileName)));
        getDAO().addCategory(null, category);
    }

}
