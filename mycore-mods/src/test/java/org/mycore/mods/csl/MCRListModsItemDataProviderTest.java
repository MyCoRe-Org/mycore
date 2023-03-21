package org.mycore.mods.csl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCRListModsItemDataProviderTest extends MCRMODSCSLTest {

    @Test
    public void testSorting() throws IOException, JDOMException, SAXException {
        List<MCRContent> testContent = new ArrayList<>(getTestContent());
        MCRListModsItemDataProvider mcrListModsItemDataProvider = new MCRListModsItemDataProvider();

        for (int i = 0; i < 10; i++) {
            Collections.shuffle(testContent);
            Element root = new Element("root");
            for (MCRContent c : testContent) {
                root.addContent(c.asXML().getRootElement().clone());
            }
            mcrListModsItemDataProvider.addContent(new MCRJDOMContent(root));

            List<String> ids = mcrListModsItemDataProvider.getIds().stream().toList();
            Assert.assertEquals("The number of ids should match the number of input", testContent.size(), ids.size());
            for (int j = 0; j < ids.size(); j++) {
                String id = ids.get(j);
                String idFromContent = testContent.get(j).asXML().getRootElement().getAttributeValue("ID");
                Assert.assertEquals("The order of output should match input", idFromContent, id);
            }
            mcrListModsItemDataProvider.reset();
        }
    }

}
