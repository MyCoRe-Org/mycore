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

package org.mycore.solr.search;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryParser;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRQLSearchUtilsTest extends MCRTestCase {
    /**
     * Test method for
     * {@link org.mycore.solr.search.MCRQLSearchUtils#getSolrQuery(org.mycore.services.fieldquery.MCRQuery, org.jdom2.Document, javax.servlet.http.HttpServletRequest)}
     * .
     */
    @Test
    public final void testGetSolrQuery() {
        MCRQuery andQuery = getMCRQuery("(state = \"submitted\") AND (state = \"published\")");
        assertEquals("+state:\"submitted\" +state:\"published\"",
            MCRQLSearchUtils.getSolrQuery(andQuery, andQuery.buildXML(), null).getQuery());
        //MCR-994
        MCRQuery orQuery = getMCRQuery("(state = \"submitted\") OR (state = \"published\")");
        assertEquals("+(state:\"submitted\" state:\"published\")",
            MCRQLSearchUtils.getSolrQuery(orQuery, orQuery.buildXML(), null).getQuery());
    }

    private MCRQuery getMCRQuery(String mcrql) {
        LogManager.getLogger(getClass()).info("Building query from condition: {}", mcrql);
        Element query = new Element("query").setAttribute("numPerPage", "20");
        Element conditions = new Element("conditions").setAttribute("format", "xml");
        MCRCondition<Void> condition = new MCRQueryParser().parse(mcrql);
        query.addContent(conditions);
        conditions.addContent(condition.toXML());
        Document queryDoc = new Document(query);
        return MCRQLSearchUtils.buildFormQuery(queryDoc.getRootElement());
    }

}
