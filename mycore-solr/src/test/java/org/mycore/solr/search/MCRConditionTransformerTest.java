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

package org.mycore.solr.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRConditionTransformerTest {

    @Test
    final void testToSolrQueryString() {
        HashSet<String> usedFields = new HashSet<>();
        MCRQueryCondition inner1 = new MCRQueryCondition("objectType", "=", "mods");
        assertEquals("+objectType:\"mods\"", MCRConditionTransformer.toSolrQueryString(inner1, usedFields));
        assertTrue(usedFields.contains("objectType"), "usedFields did not contain 'objectType'");
        MCRQueryCondition inner2 = new MCRQueryCondition("objectType", "=", "cbu");
        MCROrCondition<Void> orCond = new MCROrCondition<>(inner1, inner2);
        usedFields.clear();
        //MCR-973 check for surrounding brackets on single OR query 
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\")",
            MCRConditionTransformer.toSolrQueryString(orCond, usedFields));
        assertTrue(usedFields.contains("objectType"), "usedFields did not contain 'objectType'");
        MCRQueryCondition inner3 = new MCRQueryCondition("derCount", ">", "0");
        MCRAndCondition<Void> andCondition = new MCRAndCondition<>(orCond, inner3);
        usedFields.clear();
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:{0 TO *]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        assertTrue(usedFields.contains("objectType"), "usedFields did not contain 'objectType'");
        assertTrue(usedFields.contains("derCount"), "usedFields did not contain 'derCount'");
        inner3.setOperator(">=");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[0 TO *]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        inner3.setOperator("<");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0}",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        inner3.setOperator("<=");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        MCRNotCondition<Void> notCond = new MCRNotCondition<>(orCond);
        andCondition.getChildren().removeFirst();
        andCondition.getChildren().addFirst(notCond);
        assertEquals("-(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
    }

}
