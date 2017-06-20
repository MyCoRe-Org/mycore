package org.mycore.mets.model.converter;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRJSONSimpleModelConverterTest {

    private String json;

    @Before
    public void buildJson() throws IOException {
        json = MCRMetsTestUtil.readJsonFile("text-json-1.json");
    }

    @Test
    public void testToSimpleModel() throws Exception {
        MCRMetsSimpleModel metsSimpleModel = MCRJSONSimpleModelConverter.toSimpleModel(json);
        MCRMetsSimpleModel compareSimpleModel = MCRMetsTestUtil.buildMetsSimpleModel();

        MCRMetsSection s1RootSection = metsSimpleModel.getRootSection();
        MCRMetsSection s2RootSection = compareSimpleModel.getRootSection();

        Assert.assertEquals("labels of root must be the same", s1RootSection.getLabel(), s2RootSection.getLabel());
        Assert.assertEquals("types of root must be the same", s1RootSection.getType(), s2RootSection.getType());

        Assert.assertEquals("page count must be the same", metsSimpleModel.getMetsPageList().size(),
            compareSimpleModel.getMetsPageList().size());

        List<MCRMetsLink> s1SectionPageLinkList = metsSimpleModel.getSectionPageLinkList();
        List<MCRMetsLink> s2SectionPageLinkList = compareSimpleModel.getSectionPageLinkList();

        for (int n = 0; n < 3; n++) {
            Assert.assertEquals("from of " + n + " link must be the same",
                s1SectionPageLinkList.get(n).getFrom().getLabel(), s2SectionPageLinkList.get(n).getFrom().getLabel());
            Assert.assertEquals("to of " + n + " link must be the same",
                s1SectionPageLinkList.get(n).getTo().getOrderLabel(),
                s2SectionPageLinkList.get(n).getTo().getOrderLabel());
        }
    }
}
