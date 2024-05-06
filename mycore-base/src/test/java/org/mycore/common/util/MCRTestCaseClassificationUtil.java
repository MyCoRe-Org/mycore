package org.mycore.common.util;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

public class MCRTestCaseClassificationUtil {
    /**
     * Adds a classification represented by the given XML file to the system.
     *
     * @param file the XML file containing the classification
     */
    public static void addClassification(String file) throws Exception {
        Document classification = new SAXBuilder().build(MCRTestCaseClassificationUtil.class.getResourceAsStream(file));
        MCRCategory category = MCRXMLTransformer.getCategory(classification);
        MCRCategoryDAOFactory.getInstance().addCategory(null, category);
    }

}
