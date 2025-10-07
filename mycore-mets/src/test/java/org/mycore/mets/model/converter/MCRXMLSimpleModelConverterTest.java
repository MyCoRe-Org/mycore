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

package org.mycore.mets.model.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jdom2.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.mets.model.MCRMetsModelHelper;
import org.mycore.mets.model.simple.MCRMetsFile;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRXMLSimpleModelConverterTest {

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

    private MCRMetsSimpleModel metsSimpleModel;

    @BeforeEach
    public void readMetsSimpleModel() throws Exception {
        Document document = MCRMetsTestUtil.readXMLFile("test-mets-1.xml");
        metsSimpleModel = MCRXMLSimpleModelConverter.fromXML(document);
    }

    @Test
    public void testFromXMLMetsFiles() {
        Iterator<MCRMetsFile> hrefIterator = metsSimpleModel.getMetsPageList()
            .stream()
            .map((p) -> {
                Optional<MCRMetsFile> first = p.getFileList().stream().findFirst();
                return first.get();
            }).iterator();

        int i = 0;
        while (hrefIterator.hasNext()) {
            i++;
            MCRMetsFile mcrMetsFile = hrefIterator.next();
            String expectedFileName = String.format(FILE_NAME_MATCH_PATTERN, i);

            String message = String.format("href %s should match %s", mcrMetsFile.getHref(), expectedFileName);
            assertEquals(mcrMetsFile.getHref(), expectedFileName, message);

            message = String.format("MimeType %s should match %s", mcrMetsFile.getMimeType(), ALL_FILE_MIMETYPE);
            assertEquals(ALL_FILE_MIMETYPE, mcrMetsFile.getMimeType(), message);

            message = String.format("File-Use %s should match %s", mcrMetsFile.getUse(), MCRMetsModelHelper.MASTER_USE);
            assertEquals(MCRMetsModelHelper.MASTER_USE, mcrMetsFile.getUse(), message);
        }
    }

    @Test
    public void testFromXMLMetsSection() {
        MCRMetsSection rootSection = metsSimpleModel.getRootSection();
        assertEquals(ROOT_SECTION_LABEL, rootSection.getLabel(),
            "The rootSection label should be " + ROOT_SECTION_LABEL);

        assertEquals("log_ArchNachl_derivate_00000011", rootSection.getId());

        List<MCRMetsSection> metsSectionList = rootSection.getMetsSectionList();

        String message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "first", ROOT_SECTION_FIRST_CHILD_LABEL,
            "label");
        assertEquals(ROOT_SECTION_FIRST_CHILD_LABEL, metsSectionList.getFirst().getLabel(), message);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "first", COVER_FRONT_TYPE, "type");
        assertEquals(COVER_FRONT_TYPE, metsSectionList.get(0).getType(), message);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "second", ROOT_SECTION_FIRST_CHILD_LABEL, "label");
        assertEquals(ROOT_SECTION_SECOND_CHILD_LABEL, metsSectionList.get(1).getLabel(), message);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "second", CONTAINED_WORK_TYPE, "type");
        assertEquals(CONTAINED_WORK_TYPE, metsSectionList.get(1).getType(), message);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "third", ROOT_SECTION_THIRD_CHILD_LABEL, "label");
        assertEquals(ROOT_SECTION_THIRD_CHILD_LABEL, metsSectionList.get(2).getLabel(), message);

        message = String.format(SECTION_ASSERT_MESSAGE_PATTERN, "third", CHAPTER_TYPE, "type");
        assertEquals(CHAPTER_TYPE, metsSectionList.get(2).getType(), message);
    }

}
