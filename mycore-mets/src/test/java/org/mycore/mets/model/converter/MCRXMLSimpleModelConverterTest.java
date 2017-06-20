package org.mycore.mets.model.converter;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.junit.Assert;
import org.junit.Before;
import org.mycore.mets.model.simple.MCRMetsFile;
import org.mycore.mets.model.simple.MCRMetsFileUse;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRXMLSimpleModelConverterTest {

    public static final Logger LOGGER = LogManager.getLogger(MCRXMLSimpleModelConverterTest.class);

    public static final String FILE_NAME_MATCH_PATTERN = "file%d.jpg";

    public static final String ROOT_SECTION_LABEL = "ArchNachl_derivate_00000011";

    public static final String ROOT_SECTION_FIRST_CHILD_LABEL = "intro";

    public static final String ROOT_SECTION_SECOND_CHILD_LABEL = "begin";

    public static final String ROOT_SECTION_THIRD_CHILD_LABEL = "middle";

    public static final String SECTION_ASSERT_MESSAGE_PATTERN = "The %s child of rootSection should have %s as %s!";

    public static final String CHAPTER_TYPE = "chapter";

    public static final String COVER_FRONT_TYPE = "- cover_front";

    public static final String CONTAINED_WORK_TYPE = "contained_work";

    public static final String ALL_FILE_MIMETYPE = "image/jpeg";

    public static final MCRMetsFileUse MASTER = MCRMetsFileUse.MASTER;

    private MCRMetsSimpleModel metsSimpleModel;

    @Before
    public void readMetsSimpleModel() throws Exception {
        Document document = MCRMetsTestUtil.readXMLFile("test-mets-1.xml");
        metsSimpleModel = MCRXMLSimpleModelConverter.fromXML(document);
    }

    @org.junit.Test
    public void testFromXMLMetsFiles() throws Exception {
        Iterator<MCRMetsFile> hrefIterator = metsSimpleModel.getMetsPageList()
            .stream()
            .map((p) -> {
                Optional<MCRMetsFile> first = p.getFileList().stream().findFirst();
                return first.get();
            }).iterator();

        int i = 0;
        while (hrefIterator.hasNext()) {
            i++;
            MCRMetsFile MCRMetsFile = hrefIterator.next();
            String expectedFileName = String.format(FILE_NAME_MATCH_PATTERN, i);

            String message = String.format("href %s should match %s", MCRMetsFile.getHref(), expectedFileName);
            Assert.assertEquals(message, MCRMetsFile.getHref(), expectedFileName);

            message = String.format("MimeType %s should match %s", MCRMetsFile.getMimeType(), ALL_FILE_MIMETYPE);
            Assert.assertEquals(message, MCRMetsFile.getMimeType(), ALL_FILE_MIMETYPE);

            message = String.format("File-Use %s should match %s", MCRMetsFile.getUse(), MASTER);
            Assert.assertEquals(message, MCRMetsFile.getUse(), MASTER);
        }
    }

    @org.junit.Test
    public void testFromXMLMetsSection() throws Exception {
        MCRMetsSection rootSection = metsSimpleModel.getRootSection();
        Assert.assertEquals("The rootSection label should be " + ROOT_SECTION_LABEL, rootSection.getLabel(),
            ROOT_SECTION_LABEL);

        List<MCRMetsSection> metsSectionList = rootSection.getMetsSectionList();

        String message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "first", ROOT_SECTION_FIRST_CHILD_LABEL,
            "label");
        Assert.assertEquals(message, metsSectionList.get(0).getLabel(), ROOT_SECTION_FIRST_CHILD_LABEL);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "first", COVER_FRONT_TYPE, "type");
        Assert.assertEquals(message, metsSectionList.get(0).getType(), COVER_FRONT_TYPE);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "second", ROOT_SECTION_FIRST_CHILD_LABEL, "label");
        Assert.assertEquals(message, metsSectionList.get(1).getLabel(), ROOT_SECTION_SECOND_CHILD_LABEL);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "second", CONTAINED_WORK_TYPE, "type");
        Assert.assertEquals(message, metsSectionList.get(1).getType(), CONTAINED_WORK_TYPE);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "third", ROOT_SECTION_THIRD_CHILD_LABEL, "label");
        Assert.assertEquals(message, metsSectionList.get(2).getLabel(), ROOT_SECTION_THIRD_CHILD_LABEL);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "third", CHAPTER_TYPE, "type");
        Assert.assertEquals(message, metsSectionList.get(2).getType(), CHAPTER_TYPE);
    }

}
