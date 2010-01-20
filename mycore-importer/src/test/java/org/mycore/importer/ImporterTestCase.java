package org.mycore.importer;

import java.io.File;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel2;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodelManager;
import org.mycore.importer.mapping.mapper.MCRImportMapper;
import org.mycore.importer.mapping.mapper.MCRImportMapperManager;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;

public class ImporterTestCase extends MCRTestCase {

    public void testDatamodel() throws Exception {
        MCRImportMetadataResolverManager metadataResolverManager = new MCRImportMetadataResolverManager();
        MCRImportDatamodelManager dmm = new MCRImportDatamodelManager("src/test/resources", metadataResolverManager);
        MCRImportDatamodel dm1 = dmm.addDatamodel("sample-dm1.xml");
        MCRImportDatamodel dm2 = dmm.addDatamodel("sample-dm2.xml");
        
        // datamodel 1 tests
        assertEquals("MCRMetaLangText", dm1.getClassname("metaText"));
        assertEquals("MCRMetaXML", dm1.getClassname("metaXML"));
        assertEquals("MCRMetaISO8601Date", dm1.getClassname("date"));
        assertEquals("MCRMetaLinkID", dm1.getClassname("link"));

        assertEquals("def.metaText", dm1.getEnclosingName("metaText"));
        assertEquals("def.metaXML", dm1.getEnclosingName("metaXML"));
        assertEquals("dates", dm1.getEnclosingName("date"));
        assertEquals("def.link", dm1.getEnclosingName("link"));

        // datamodel 2 tests
        assertEquals("MCRMetaLangText", dm2.getClassname("metaText"));
        assertEquals("MCRMetaXML", dm2.getClassname("metaXML"));
        assertEquals("MCRMetaISO8601Date", dm2.getClassname("date"));
        assertEquals("MCRMetaLinkID", dm2.getClassname("link"));

        assertEquals("def.metaText", dm2.getEnclosingName("metaText"));
        assertEquals("def.metaXML", dm2.getEnclosingName("metaXML"));
        assertEquals("dates", dm2.getEnclosingName("date"));
        assertEquals("def.link", dm2.getEnclosingName("link"));

        assertEquals("text", ((MCRImportDatamodel2)dm2).getType("metaText"));
        assertEquals("xml", ((MCRImportDatamodel2)dm2).getType("metaXML"));
        assertEquals("date", ((MCRImportDatamodel2)dm2).getType("date"));
        assertEquals("link", ((MCRImportDatamodel2)dm2).getType("link"));
    }

    public void testMapperManager() throws Exception {
        MCRImportMapperManager mm = new MCRImportMapperManager();
        mm.addMapper("testMapper", TestMapper.class);
        MCRImportMapper mapper = mm.createMapperInstance("testMapper");
        assertNotNull(mapper);
        assertEquals("testMapper", mapper.getType());
    }
    public static class TestMapper implements MCRImportMapper {
        public String getType() {
            return "testMapper";
        }
        public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {}
    }

    public void testMetadataResolver() throws Exception {
        MCRImportMetadataResolverManager metadataResolverManager = new MCRImportMetadataResolverManager();
        metadataResolverManager.addToResolverTable("testMetadata", "MCRTestMetadata", TestMetadataResolver.class);
        assertEquals(metadataResolverManager.getClassNameByType("testMetadata"), "MCRTestMetadata");
        assertEquals(metadataResolverManager.getTypeByClassName("MCRTestMetadata"), "testMetadata");

        MCRImportMetadataResolver metadataResolver = metadataResolverManager.createInstance("testMetadata");
        Element saveToElement = new Element("testRoot");
        metadataResolver.resolve(null, null, saveToElement);
        assertEquals("1", saveToElement.getAttributeValue("test"));
    }
    public static class TestMetadataResolver implements MCRImportMetadataResolver {
        public boolean resolve(Element map, List<MCRImportField> fieldList, Element saveToElement) {
            saveToElement.setAttribute("test", "1");
            return true;
        }
    }

    public void testConfig() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File("src/test/resources/sample-mapping.xml"));
        MCRImportConfig config = new MCRImportConfig(doc.getRootElement());

        assertEquals("sample", config.getProjectName());
        assertEquals("save/", config.getSaveToPath());
        assertEquals(true, config.isCreateClassificationMapping());
        assertEquals(true, config.isUseDerivates());
        assertEquals(true, config.isCreateInImportDir());
        assertEquals(true, config.isImportToMycore());
        assertEquals(true, config.isImportFilesToMycore());
    }
    
    public void testFieldsAndRecords() throws Exception {
        MCRImportField id = new MCRImportField("id", "000001");

        MCRImportField textField1 = new MCRImportField("text", "Beispiel Text");
        MCRImportField textField2 = new MCRImportField("text", "und noch ein Text");
        MCRImportField dateField = new MCRImportField("date", "2001-10-23");

        MCRImportField linkHrefField = new MCRImportField("link_href", "HrefOfLink");
        MCRImportField linkLabelField = new MCRImportField("link_label", "Ein Link");

        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(id);
        record.addField(textField1);
        record.addField(textField2);
        record.addField(dateField);
        record.addField(linkHrefField);
        record.addField(linkLabelField);

        assertEquals(6, record.getFields().size());
        assertEquals(true, MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping.xml")));
        MCRImportObject importObject = MCRImportMappingManager.getInstance().createMCRObject(record);
        assertEquals("000001", importObject.getId());
        assertEquals("MCRMetaLangText", importObject.getMetadata("metaText").getClassName());
        assertEquals(2, importObject.getMetadata("metaText").getChilds().size());
        assertEquals("2001-10-23", importObject.getMetadata("date").getChilds().get(0).getText());
        MCRImportMappingManager.getInstance().saveImportObject(importObject, "xml");
        assertEquals(true, new File("save/xml/000001.xml").exists());

        record = new MCRImportRecord("record");
        record.addField(new MCRImportField("text", "DM1 and ID tests!"));
        assertEquals(true, MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping2.xml")));
        importObject = MCRImportMappingManager.getInstance().mapAndSaveRecord(record);
        // automatic generated id
        assertEquals("record_0", importObject.getId());
        assertEquals("DM1 and ID tests!", importObject.getLabel());
        assertEquals("Sample: DM1 and ID tests!", importObject.getMetadata("metaText").getChilds().get(0).getText());
    }

}