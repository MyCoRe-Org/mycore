package org.mycore.datamodel.classifications2.impl;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRAbstractCategoryImplTest extends MCRTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("MCR.Metadata.DefaultLang.foo", "true");
    }

    @After
    public void clean() {
        System.setProperty("MCR.Metadata.DefaultLang.foo", "false");
    }

    @Test
    public void getCurrentLabel() {
        MCRCategory cat = new MCRSimpleAbstractCategoryImpl();
        MCRLabel label1 = new MCRLabel("de", "german", null);
        MCRLabel label2 = new MCRLabel("fr", "french", null);
        MCRLabel label3 = new MCRLabel("at", "austrian", null);
        cat.getLabels().add(label1);
        cat.getLabels().add(label2);
        cat.getLabels().add(label3);
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setCurrentLanguage("en");
        assertEquals("German label expected", label3, cat.getCurrentLabel().get());
        cat.getLabels().clear();
        cat.getLabels().add(label2);
        cat.getLabels().add(label3);
        cat.getLabels().add(label1);
        assertEquals("German label expected", label3, cat.getCurrentLabel().get());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.DefaultLang", "at");
        return testProperties;
    }

}
