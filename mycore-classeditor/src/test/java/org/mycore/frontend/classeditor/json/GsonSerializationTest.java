package org.mycore.frontend.classeditor.json;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;

public class GsonSerializationTest {
    @Before
    public void init() {
        MCRConfiguration mcrProperties = MCRConfiguration.instance();
        mcrProperties.initialize(MCRConfigurationLoaderFactory.getConfigurationLoader().load(), true);
        mcrProperties.set("MCR.Category.DAO", CategoryDAOMock.class.getName());
    }

    protected MCRCategoryImpl createCateg(String rootID, String id2, String text) {
        MCRCategoryImpl mcrCategoryImpl = new MCRCategoryImpl();
        MCRCategoryID id = new MCRCategoryID(rootID, id2);
        mcrCategoryImpl.setId(id);
        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("de", text + "_de", "desc_" + text + "_de"));
        labels.add(new MCRLabel("en", text + "_en", "desc_" + text + "_en"));
        mcrCategoryImpl.setLabels(labels);
        return mcrCategoryImpl;
    }

}
