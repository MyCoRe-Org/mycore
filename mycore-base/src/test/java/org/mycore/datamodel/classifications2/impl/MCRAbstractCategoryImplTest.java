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

package org.mycore.datamodel.classifications2.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.DefaultLang.foo", string = "true")
    })
public class MCRAbstractCategoryImplTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "MCR.Metadata.DefaultLang", string = "at")
        })
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
        assertEquals(label3, cat.getCurrentLabel().get(), "German label expected");
        cat.getLabels().clear();
        cat.getLabels().add(label2);
        cat.getLabels().add(label3);
        cat.getLabels().add(label1);
        assertEquals(label3, cat.getCurrentLabel().get(), "German label expected");
    }

}
