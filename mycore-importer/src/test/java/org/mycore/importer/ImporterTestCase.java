package org.mycore.importer;

import org.jdom.Element;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodelManager;
import org.mycore.importer.mapping.mapper.MCRImportMapper;
import org.mycore.importer.mapping.mapper.MCRImportMapperManager;

public class ImporterTestCase extends MCRTestCase {

    public void testDatamodel() {
        MCRImportDatamodelManager dmm = new MCRImportDatamodelManager();
//        dmm.getDatamodel(datamodelPath);
    }

    public void testMapperManager() throws Exception {
        MCRImportMapperManager mm = new MCRImportMapperManager();
        mm.addMapper("testMapper", TestMapper.class);
        MCRImportMapper mapper = mm.createMapperInstance("testMapper");
        assertNotNull(mapper);
    }
    public static class TestMapper implements MCRImportMapper {
        public String getType() {
            return "testMapper";
        }
        public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
            
        }
    }

}
