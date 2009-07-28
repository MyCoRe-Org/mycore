package org.mycore.datamodel.classifications2.impl;


import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRAbstractCategoryImplTest extends MCRTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.DefaultLang", "de", true);
    }
    
    public void testGetCurrentLabel(){
        MCRCategory cat=new MCRSimpleAbstractCategoryImpl();
        MCRLabel label1=new MCRLabel("de", "german", null);
        MCRLabel label2=new MCRLabel("fr", "french", null);
        MCRLabel label3=new MCRLabel("at", "austrian", null);
        cat.getLabels().add(label1);
        cat.getLabels().add(label2);
        cat.getLabels().add(label3);
        MCRSession session=MCRSessionMgr.getCurrentSession();
        session.setCurrentLanguage("en");
        assertEquals("German label expected", cat.getCurrentLabel(), label1);
        cat.getLabels().clear();
        cat.getLabels().add(label2);
        cat.getLabels().add(label3);
        cat.getLabels().add(label1);
        assertEquals("German label expected", cat.getCurrentLabel(), label1);
    }

}
