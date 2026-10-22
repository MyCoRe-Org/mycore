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

package org.mycore.datamodel.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

public class MCRLanguageFactoryTest extends MCRJPATestCase {

    private static final String CLASSIFICATION_ID = "rfc5646";

    private static final String LANGUAGE_CODE = "tlh";

    private static final String LANGUAGE_LABEL = "Klingonisch";

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.LanguageClassification", CLASSIFICATION_ID);
        return testProperties;
    }

    /**
     * Adds a language to the classification, so the factory reads the classification again on next use.
     */
    @Before
    public void addLanguageToClassification() {
        MCRCategoryID rootID = MCRCategoryID.rootID(CLASSIFICATION_ID);
        MCRCategory root = new MCRCategoryImpl();
        root.setId(rootID);
        MCRCategory language = new MCRCategoryImpl();
        language.setId(new MCRCategoryID(CLASSIFICATION_ID, LANGUAGE_CODE));
        language.getLabels().add(new MCRLabel("de", LANGUAGE_LABEL, null));
        MCRCategoryDAOFactory.getInstance().addCategory(null, root);
        MCRCategoryDAOFactory.getInstance().addCategory(rootID, language);
        startNewTransaction();
    }

    @Test
    public void readsTheLanguageClassificationIfSessionCreationIsUnlocked() throws Exception {
        runInNewThread(() -> {
            MCRSessionMgr.unlock();
            try {
                assertLanguageFromClassification();
                assertFalse(MCRSessionMgr.isLocked());
                assertFalse(MCRTransactionHelper.isTransactionActive());
            } finally {
                releaseAndCloseCurrentSession();
            }
        });
    }

    @Test
    public void readsTheLanguageClassificationWithSession() throws Exception {
        runInNewThread(() -> {
            MCRSessionMgr.unlock();
            MCRSession session = MCRSessionMgr.getCurrentSession();
            try {
                assertLanguageFromClassification();
                assertSame(session, MCRSessionMgr.getCurrentSession());
                assertFalse(MCRTransactionHelper.isTransactionActive());
            } finally {
                releaseAndCloseCurrentSession();
            }
        });
    }

    private static void assertLanguageFromClassification() {
        MCRLanguage language = MCRLanguageFactory.instance().getLanguage(LANGUAGE_CODE);
        assertEquals(LANGUAGE_LABEL, language.getLabel("de"));
    }

    private static void runInNewThread(Runnable runnable) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(runnable).get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void releaseAndCloseCurrentSession() {
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            MCRSessionMgr.releaseCurrentSession();
            session.close();
        }
    }
}
