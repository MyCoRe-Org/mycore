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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

public class MCRLanguageFactoryTest extends MCRJPATestCase {

    private static final String CLASSIFICATION_ID = "rfc5646";

    private static final String LANGUAGE_CODE = "tlh";

    private static final String LANGUAGE_LABEL = "Klingonisch";

    /**
     * Adds a language to the classification, so the factory reads the classification again on next use.
     */
    @Before
    public void addLanguageToClassification() {
        MCRCategoryID rootID = new MCRCategoryID(CLASSIFICATION_ID);
        MCRCategory root = new MCRCategoryImpl();
        root.setId(rootID);
        MCRCategory language = new MCRCategoryImpl();
        language.setId(new MCRCategoryID(CLASSIFICATION_ID, LANGUAGE_CODE));
        language.getLabels().add(new MCRLabel("de", LANGUAGE_LABEL, null));
        MCRCategoryDAOFactory.obtainInstance().addCategory(null, root);
        MCRCategoryDAOFactory.obtainInstance().addCategory(rootID, language);
        startNewTransaction();
    }

    @Test
    public void readsTheLanguageClassificationIfSessionCreationIsUnlocked() throws Exception {
        runInNewThread(() -> {
            MCRSessionMgr.unlock();
            try {
                assertLanguageFromClassification();
                assertFalse(MCRSessionMgr.isLocked());
                assertFalse(MCRTransactionManager.hasActiveTransactions());
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
                assertFalse(MCRTransactionManager.hasActiveTransactions());
            } finally {
                releaseAndCloseCurrentSession();
            }
        });
    }

    @Test
    public void readsTheLanguageClassificationWithoutSession() throws Exception {
        runInNewThread(() -> {
            assertTrue(MCRSessionMgr.isLocked());
            assertFalse(MCRSessionMgr.hasCurrentSession());
            assertLanguageFromClassification();
            assertTrue(MCRSessionMgr.isLocked());
            assertFalse(MCRSessionMgr.hasCurrentSession());
        });
    }

    @Test
    public void readsTheLanguageClassificationInActiveTransactionWithoutSession() throws Exception {
        runInNewThread(() -> {
            assertTrue(MCRSessionMgr.isLocked());
            MCRTransactionManager.beginTransactions();
            try {
                assertLanguageFromClassification();
                assertTrue(MCRTransactionManager.hasActiveTransactions());
                assertFalse(MCRSessionMgr.hasCurrentSession());
            } finally {
                MCRTransactionManager.rollbackTransactions();
            }
        });
    }

    private static void assertLanguageFromClassification() {
        // own instance: the shared one may have been created by another test without the language classification
        MCRLanguageFactory factory = new MCRLanguageFactory("de", new MCRCategoryID(CLASSIFICATION_ID));
        MCRLanguage language = factory.getLanguage(LANGUAGE_CODE);
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
