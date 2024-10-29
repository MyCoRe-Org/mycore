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

package org.mycore.common.util;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

public class MCRTestCaseClassificationUtil {
    /**
     * Adds a classification represented by the given XML file from classpath to the system.
     *
     * @param resourcePath the XML classpath file containing the classification
     */
    public static void addClassification(String resourcePath) throws Exception {
        Document classification = new SAXBuilder()
            .build(MCRTestCaseClassificationUtil.class.getResourceAsStream(resourcePath));
        MCRCategory category = MCRXMLTransformer.getCategory(classification);
        MCRCategoryDAOFactory.getInstance().addCategory(null, category);
    }

}
