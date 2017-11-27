/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
