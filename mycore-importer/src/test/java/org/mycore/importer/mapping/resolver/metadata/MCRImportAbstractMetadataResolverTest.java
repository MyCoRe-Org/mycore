package org.mycore.importer.mapping.resolver.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.MCRImportField;

public class MCRImportAbstractMetadataResolverTest extends MCRTestCase {

    private Document doc;
    private TestResolver res;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        res = new TestResolver();
        SAXBuilder saxBuilder = new SAXBuilder();
        doc = saxBuilder.build(new File("src/test/resources/sample-mapping.xml"));
    }

    @Test
    public void resolveCondition() throws Exception {
        Element map = (Element)XPath.selectSingleNode(doc.getRootElement(),
                "/import/mapping/mcrobjects/mcrobject/map[@fields='field,condition']");

        MCRImportField condition = new MCRImportField("condition", "5");
        MCRImportField field = new MCRImportField("field", "sample text");
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        fieldList.add(condition);
        fieldList.add(field);
        boolean valid = res.resolve(map, fieldList, new Element("saveTo"));
        assertEquals(true, valid);
        assertEquals(2, res.getConditions().size());
        assertNotNull(res.getConditions().get("_default"));
        assertNotNull(res.getConditions().get("conditionWithName"));
    }

    public class TestResolver extends MCRImportAbstractMetadataResolver {
        @Override
        public boolean isValid() {
            return true;
        }
    }
}
