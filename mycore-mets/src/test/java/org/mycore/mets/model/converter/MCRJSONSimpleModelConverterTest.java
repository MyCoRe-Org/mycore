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

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRJSONSimpleModelConverterTest {

    private String json;

    @BeforeEach
    public void buildJson() throws IOException {
        json = MCRMetsTestUtil.readJsonFile("text-json-1.json");
    }

    @Test
    public void testToSimpleModel() {
        MCRMetsSimpleModel metsSimpleModel = MCRJSONSimpleModelConverter.toSimpleModel(json);
        MCRMetsSimpleModel compareSimpleModel = MCRMetsTestUtil.buildMetsSimpleModel();

        MCRMetsSection s1RootSection = metsSimpleModel.getRootSection();
        MCRMetsSection s2RootSection = compareSimpleModel.getRootSection();

        assertEquals(s1RootSection.getLabel(), s2RootSection.getLabel(),
            "labels of root must be the same");
        assertEquals(s1RootSection.getType(), s2RootSection.getType(),
            "types of root must be the same");

        assertEquals(metsSimpleModel.getMetsPageList().size(), compareSimpleModel.getMetsPageList().size(),
            "page count must be the same");

        List<MCRMetsLink> s1SectionPageLinkList = metsSimpleModel.getSectionPageLinkList();
        List<MCRMetsLink> s2SectionPageLinkList = compareSimpleModel.getSectionPageLinkList();

        for (int n = 0; n < 3; n++) {
            assertEquals(s1SectionPageLinkList.get(n).getFrom().getLabel(),
                s2SectionPageLinkList.get(n).getFrom().getLabel(),
                "from of " + n + " link must be the same");
            assertEquals(s1SectionPageLinkList.get(n).getTo().getOrderLabel(),
                s2SectionPageLinkList.get(n).getTo().getOrderLabel(),
                "to of " + n + " link must be the same");
        }
    }
}
