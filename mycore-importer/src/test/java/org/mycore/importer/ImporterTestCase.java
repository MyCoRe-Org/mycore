package org.mycore.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.importer.classification.MCRImportClassificationMappingManager;
import org.mycore.importer.derivate.MCRImportDerivate;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel2;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodelManager;
import org.mycore.importer.mapping.mapper.MCRImportMapper;
import org.mycore.importer.mapping.mapper.MCRImportMapperManager;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;
import org.mycore.importer.mcrimport.MCRImportImporter;

public class ImporterTestCase extends MCRTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.basedir", "src", true);
    }

    @Test
    public void datamodel() throws Exception {
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

        assertEquals(true, dm1.isRequired("metaText"));
        assertEquals(true, dm1.isRequired("metaXML"));
        assertEquals(false, dm1.isRequired("date"));
        assertEquals(false, dm1.isRequired("link"));

        assertEquals(4, dm1.getMetadataNames().size());

        // datamodel 2 tests
        assertEquals("MCRMetaLangText", dm2.getClassname("metaText"));
        assertEquals("MCRMetaXML", dm2.getClassname("metaXML"));
        assertEquals("MCRMetaISO8601Date", dm2.getClassname("date"));
        assertEquals("MCRMetaLinkID", dm2.getClassname("link"));
        assertEquals("MCRMetaClassification", dm2.getClassname("class"));

        assertEquals("def.metaText", dm2.getEnclosingName("metaText"));
        assertEquals("def.metaXML", dm2.getEnclosingName("metaXML"));
        assertEquals("dates", dm2.getEnclosingName("date"));
        assertEquals("def.link", dm2.getEnclosingName("link"));

        assertEquals("text", ((MCRImportDatamodel2) dm2).getType("metaText"));
        assertEquals("xml", ((MCRImportDatamodel2) dm2).getType("metaXML"));
        assertEquals("date", ((MCRImportDatamodel2) dm2).getType("date"));
        assertEquals("link", ((MCRImportDatamodel2) dm2).getType("link"));
        assertEquals("classification", ((MCRImportDatamodel2) dm2).getType("class"));

        assertEquals(true, dm2.isRequired("metaText"));
        assertEquals(true, dm2.isRequired("metaXML"));
        assertEquals(false, dm2.isRequired("date"));
        assertEquals(false, dm2.isRequired("link"));
        assertEquals(false, dm2.isRequired("class"));

        assertEquals(5, dm2.getMetadataNames().size());
    }

    @Test
    public void mapperManager() throws Exception {
        MCRImportMapperManager mm = new MCRImportMapperManager();
        mm.addMapper("testMapper", TestMapper.class);
        MCRImportMapper mapper = mm.createMapperInstance("testMapper");
        assertNotNull(mapper);
        assertEquals("testMapper", mapper.getType());
    }

    @Test
    public void metadataResolver() throws Exception {
        MCRImportMetadataResolverManager metadataResolverManager = new MCRImportMetadataResolverManager();
        metadataResolverManager.addToResolverTable("testMetadata", "MCRTestMetadata", TestMetadataResolver.class);
        assertEquals(metadataResolverManager.getClassNameByType("testMetadata"), "MCRTestMetadata");
        assertEquals(metadataResolverManager.getTypeByClassName("MCRTestMetadata"), "testMetadata");

        MCRImportMetadataResolver metadataResolver = metadataResolverManager.createInstance("testMetadata");
        Element saveToElement = new Element("testRoot");
        metadataResolver.resolve(null, null, saveToElement);
        assertEquals("1", saveToElement.getAttributeValue("test"));
    }

    @Test
    public void config() throws Exception {
        MCRConfiguration.instance().set("MCR.basedir", "src");
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File("src/test/resources/sample-mapping.xml"));
        MCRImportConfig config = new MCRImportConfig(doc.getRootElement());

        assertEquals("src/test/resources/", config.getDatamodelPath());
        assertEquals("sample", config.getProjectName());
        assertEquals("save/mapping/", config.getSaveToPath());
        assertEquals(true, config.isCreateClassificationMapping());
        assertEquals(true, config.isUseDerivates());
        assertEquals(true, config.isCreateInImportDir());
        assertEquals(true, config.isImportToMycore());
        assertEquals(true, config.isImportFilesToMycore());
    }

    @Test
    public void fieldsAndRecords() throws Exception {
        // create a new record
        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(new MCRImportField("id", "000001"));
        record.addField(new MCRImportField("text", "Beispiel Text"));
        record.addField(new MCRImportField("text", "und noch ein Text"));
        record.addField(new MCRImportField("date", "2001-10-23"));
        record.addField(new MCRImportField("link_href", "HrefOfLink"));
        record.addField(new MCRImportField("link_label", "Ein Link"));
        record.addField(new MCRImportField("last", "Mustermann"));
        record.addField(new MCRImportField("first", "Max"));
        assertEquals(8, record.getFields().size());

        // init mapping manager and create a new import object
        assertEquals(true, MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping.xml")));
        MCRImportObject importObject = MCRImportMappingManager.getInstance().createMCRObject(record);
        // test id, text and date
        assertEquals("000001", importObject.getId());
        assertEquals("MCRMetaLangText", importObject.getMetadata("metaText").getClassName());
        assertEquals(2, importObject.getMetadata("metaText").getChilds().size());
        assertEquals("2001-10-23", importObject.getMetadata("date").getChilds().get(0).getText());

        // test metaxml
        Element metaXML = importObject.getMetadata("metaXML").getChilds().get(0);
        assertEquals("metaXML", metaXML.getName());
        assertEquals(2, metaXML.getContent().size());
        Element lastName = (Element) metaXML.getContent(new ElementFilter("lastName")).get(0);
        Element firstName = (Element) metaXML.getContent(new ElementFilter("firstName")).get(0);
        assertEquals("Mustermann", lastName.getText());
        assertEquals("Max", firstName.getText());

        // save the import object to disk and check if the file exists
        MCRImportMappingManager.getInstance().saveImportObject(importObject, "xml");
        assertEquals(true, new File("save/mapping/xml/000001.xml").exists());

        // test id generation, label and text
        record = new MCRImportRecord("record");
        record.addField(new MCRImportField("text", "DM1 and ID tests!"));
        assertEquals(true, MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping2.xml")));
        importObject = MCRImportMappingManager.getInstance().mapAndSaveRecord(record);
        assertEquals("record_0", importObject.getId());
        assertEquals("DM1 and ID tests!", importObject.getLabel());
        assertEquals("Sample: DM1 and ID tests!", importObject.getMetadata("metaText").getChilds().get(0).getText());

        MCRUtils.deleteDirectory(new File("save"));
    }

    @Test
    public void classification() throws Exception {
        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(new MCRImportField("id", "id"));
        record.addField(new MCRImportField("categ", "category 1"));
        MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping.xml"));
        MCRImportObject importObject = MCRImportMappingManager.getInstance().createMCRObject(record);

        // test import object
        Element classification = importObject.getMetadata("class").getChilds().get(0);
        assertEquals("class", classification.getName());
        assertEquals("Sample_00001", classification.getAttributeValue("classid"));
        assertEquals("category 1", classification.getAttributeValue("categid"));

        // test classification mapping
        MCRImportClassificationMappingManager cMM = MCRImportMappingManager.getInstance().getClassificationMappingManager();
        cMM.addImportValue("Sample_00001", "category 2");
        assertEquals(true, cMM.containsImportValue("Sample_00001", "category 1"));
        assertEquals(true, cMM.containsImportValue("Sample_00001", "category 2"));
        cMM.setMyCoReValue("Sample_00001", "category 1", "mycore value 1");
        assertEquals("mycore value 1", cMM.getMyCoReValue("Sample_00001", "category 1"));
        cMM.saveAllClassificationMaps();
        File classMappingFile = new File("save/mapping/classification/Sample_00001.xml");
        assertEquals(true, classMappingFile.exists());
        SAXBuilder builder = new SAXBuilder();
        Element rootMappingElement = builder.build(classMappingFile).getRootElement();
        assertEquals("classificationMapping", rootMappingElement.getName());
        assertEquals("Sample_00001", rootMappingElement.getAttributeValue("id"));

        // reload mapping file
        cMM.init();
        assertEquals("mycore value 1", cMM.getMyCoReValue("Sample_00001", "category 1"));
        assertEquals("", cMM.getMyCoReValue("Sample_00001", "category 2"));
        MCRUtils.deleteDirectory(new File("save"));
    }

    @Test
    public void derivate() throws Exception {
        MCRImportDerivate der1 = new MCRImportDerivate("0");
        MCRImportDerivate der2 = new MCRImportDerivate("1");
        der1.setLabel("test label");
        der1.addFile("src/test/resources/pic1.jpg");
        der1.addFile("src/test/resources/pic2.jpg");
        der2.addFile("src/test/resources/pic2.jpg");

        assertEquals("pic1.jpg", der1.getMainDocument());
        assertEquals("pic2.jpg", der2.getMainDocument());
        assertEquals(2, der1.getFileSet().size());
        assertEquals(1, der2.getFileSet().size());

        MCRImportMappingManager mm = MCRImportMappingManager.getInstance();
        mm.init(new File("src/test/resources/sample-mapping.xml"));
        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(new MCRImportField("id", "recordId_1"));
        record.addField(new MCRImportField("derivId", "0"));
        record.addField(new MCRImportField("derivId", "1"));

        // create derivate- and recordlist
        List<MCRImportDerivate> derivateList = new ArrayList<MCRImportDerivate>();
        derivateList.add(der1);
        derivateList.add(der2);
        mm.setDerivateList(derivateList);
        List<MCRImportRecord> recordList = new ArrayList<MCRImportRecord>();
        recordList.add(record);
        // start mapping
        mm.startMapping(recordList);

        // test generated derivate files
        SAXBuilder builder = new SAXBuilder();
        Element der1Element = builder.build("save/mapping/derivates/0.xml").getRootElement();
        assertEquals("0", der1Element.getAttributeValue("importId"));
        assertEquals("test label", der1Element.getAttributeValue("label"));
        assertEquals("recordId_1", ((Attribute) XPath.selectSingleNode(der1Element, "linkmetas/linkmeta/@xlink:href")).getValue());
        assertEquals("pic1.jpg", ((Attribute) XPath.selectSingleNode(der1Element, "files/@mainDoc")).getValue());
        String filePath = ((Text) XPath.selectSingleNode(der1Element, "files/file[1]/text()")).getValue();
        File file = new File(filePath);
        assertEquals(true, file.exists());
        assertEquals("pic1.jpg", file.getName());
        filePath = ((Text) XPath.selectSingleNode(der1Element, "files/file[last()]/text()")).getValue();
        file = new File(filePath);
        assertEquals(true, file.exists());
        assertEquals("pic2.jpg", file.getName());

        MCRUtils.deleteDirectory(new File("save"));
    }

    @Test
    public void valid() throws Exception {
        MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping.xml"));
        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(new MCRImportField("id", "000001"));
        record.addField(new MCRImportField("text", "sample text"));
        MCRImportObject importObject = MCRImportMappingManager.getInstance().createMCRObject(record);
        assertEquals(false, importObject.isValid());
        record.addField(new MCRImportField("last", "Mustermann"));
        record.addField(new MCRImportField("first", "Max"));
        importObject = MCRImportMappingManager.getInstance().createMCRObject(record);
        assertEquals(true, importObject.isValid());
    }

    @Test
    public void importTest() throws Exception {
        File mappingFile = new File("src/test/resources/sample-mapping2.xml");
        MCRConfiguration.instance().set("MCR.Metadata.Type.sample-dm1", true);
        MCRConfiguration.instance().set("MCR.Metadata.Type.sample-dm2", true);

        // do mapping
        assertEquals(true, MCRImportMappingManager.getInstance().init(new File("src/test/resources/sample-mapping2.xml")));
        MCRImportRecord record = new MCRImportRecord("record");
        record.addField(new MCRImportField("text", "import test"));
        MCRImportMappingManager.getInstance().mapAndSaveRecord(record);
        record = new MCRImportRecord("record");
        record.addField(new MCRImportField("text", "import test2"));
        MCRImportMappingManager.getInstance().mapAndSaveRecord(record);

        // do import
        MCRImportImporter im = new MCRImportImporter(mappingFile) {
            private int count = 1;

            @Override
            protected MCRObjectID getNextFreeId(String base) {
                MCRObjectID objId = MCRObjectID.getInstance(MCRObjectID.formatID(base, count++));
                return objId;
            }
        };
        im.generateMyCoReFiles();
        List<String> commandList = im.getCommandList();
        assertEquals(2, commandList.size());
        File tempFolder = new File("save/mapping2/_temp/sample-dm1");
        assertEquals(true, tempFolder.exists());
        assertEquals(2, tempFolder.listFiles().length);

        // delete save dir
        MCRUtils.deleteDirectory(new File("save"));
    }

    public static class TestMapper implements MCRImportMapper {
        public String getType() {
            return "testMapper";
        }

        public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        }
    }

    public static class TestMetadataResolver implements MCRImportMetadataResolver {
        public boolean resolve(Element map, List<MCRImportField> fieldList, Element saveToElement) {
            saveToElement.setAttribute("test", "1");
            return true;
        }
    }
}
