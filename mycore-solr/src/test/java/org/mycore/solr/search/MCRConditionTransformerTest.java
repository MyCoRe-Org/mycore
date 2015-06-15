package org.mycore.solr.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRQueryCondition;

public class MCRConditionTransformerTest extends MCRTestCase {

    @Test
    public final void testToSolrQueryString() {
        HashSet<String> usedFields = new HashSet<>();
        MCRQueryCondition inner1 = new MCRQueryCondition("objectType", "=", "mods");
        assertEquals("+objectType:\"mods\"", MCRConditionTransformer.toSolrQueryString(inner1, usedFields));
        assertTrue("usedFields did not contain 'objectType'", usedFields.contains("objectType"));
        MCRQueryCondition inner2 = new MCRQueryCondition("objectType", "=", "cbu");
        MCROrCondition<Object> orCond = new MCROrCondition<>(inner1, inner2);
        usedFields.clear();
        //MCR-973 check for surrounding brackets on single OR query 
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\")",
            MCRConditionTransformer.toSolrQueryString(orCond, usedFields));
        assertTrue("usedFields did not contain 'objectType'", usedFields.contains("objectType"));
        MCRQueryCondition inner3 = new MCRQueryCondition("derCount", ">", "0");
        MCRAndCondition<Object> andCondition = new MCRAndCondition<>(orCond, inner3);
        usedFields.clear();
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:{0 TO *]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        assertTrue("usedFields did not contain 'objectType'", usedFields.contains("objectType"));
        assertTrue("usedFields did not contain 'derCount'", usedFields.contains("derCount"));
        inner3.setOperator(">=");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[0 TO *]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        inner3.setOperator("<");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0}",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        inner3.setOperator("<=");
        assertEquals("+(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
        MCRNotCondition<Object> notCond = new MCRNotCondition<>(orCond);
        andCondition.getChildren().remove(0);
        andCondition.getChildren().add(0, notCond);
        assertEquals("-(objectType:\"mods\" objectType:\"cbu\") +derCount:[* TO 0]",
            MCRConditionTransformer.toSolrQueryString(andCondition, usedFields));
    }

}
