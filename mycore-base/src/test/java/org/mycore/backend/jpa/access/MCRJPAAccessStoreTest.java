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

package org.mycore.backend.jpa.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRJPATestCase;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRJPAAccessStoreTest extends MCRJPATestCase {

    private static final MCRACCESSRULE TRUE_RULE = getTrueRule();

    private static final MCRACCESSRULE FALSE_RULE = getFalseRule();

    private static MCRJPAAccessStore ACCESS_STORE;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (ACCESS_STORE == null) {
            ACCESS_STORE = new MCRJPAAccessStore();
        }
        MCRHIBConnection.instance().getSession().save(TRUE_RULE);
        MCRHIBConnection.instance().getSession().save(FALSE_RULE);
        startNewTransaction();

    }

    private static MCRACCESSRULE getTrueRule() {
        MCRACCESSRULE rule = new MCRACCESSRULE();
        rule.setCreationdate(new Timestamp(new Date().getTime()));
        rule.setCreator("JUnit");
        rule.setDescription("JUnit-Test rule");
        rule.setRid("junit001");
        rule.setRule("true");
        return rule;
    }

    private static MCRACCESSRULE getFalseRule() {
        MCRACCESSRULE rule = new MCRACCESSRULE();
        rule.setCreationdate(new Timestamp(new Date().getTime()));
        rule.setCreator("JUnit");
        rule.setDescription("JUnit-Test rule");
        rule.setRid("junit002");
        rule.setRule("false");
        return rule;
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#createAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    @Test
    public void createAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        assertNotNull(
            MCRHIBConnection.instance().getSession().get(MCRACCESS.class, new MCRACCESSPK(permission, objID)));
    }

    private MCRRuleMapping addRuleMapping(final String objID, final String permission, final String rid) {
        MCRRuleMapping rulemapping = new MCRRuleMapping();
        rulemapping.setCreationdate(new Date());
        rulemapping.setCreator("JUnit");
        rulemapping.setObjId(objID);
        rulemapping.setPool(permission);
        rulemapping.setRuleId(rid);
        ACCESS_STORE.createAccessDefinition(rulemapping);
        return rulemapping;
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getRuleID(String, String)}.
     */
    @Test
    public void getRuleID() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        String rid = ACCESS_STORE.getRuleID(objID, permission);
        assertEquals(TRUE_RULE.getRid(), rid);
        rid = ACCESS_STORE.getRuleID(objID, "notdefined");
        assertEquals(null, rid);
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#deleteAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    @Test
    public void deleteAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        ACCESS_STORE.deleteAccessDefinition(ruleMapping);
        startNewTransaction();
        assertNull(MCRHIBConnection.instance().getSession().get(MCRACCESS.class, new MCRACCESSPK(permission, objID)));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#updateAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    @Test
    public void updateAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        ruleMapping.setRuleId(FALSE_RULE.getRid());
        ACCESS_STORE.updateAccessDefinition(ruleMapping);
        startNewTransaction();
        MCRACCESS access = MCRHIBConnection.instance().getSession().get(MCRACCESS.class,
            new MCRACCESSPK(permission, objID));
        assertEquals(FALSE_RULE, access.getRule());
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getAccessDefinition(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        MCRRuleMapping ruleMapping2 = ACCESS_STORE.getAccessDefinition(permission, objID);
        // We will remove milliseconds as they don't need to be saved
        assertEquals((long) Math.floor(ruleMapping.getCreationdate().getTime() / 1000), ruleMapping2.getCreationdate()
            .getTime() / 1000);
        assertEquals(ruleMapping.getCreator(), ruleMapping2.getCreator());
        assertEquals(ruleMapping.getObjId(), ruleMapping2.getObjId());
        assertEquals(ruleMapping.getPool(), ruleMapping2.getPool());
        assertEquals(ruleMapping.getRuleId(), ruleMapping2.getRuleId());
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getMappedObjectId(java.lang.String)}.
     */
    @Test
    public void getMappedObjectId() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        List<String> results = ACCESS_STORE.getMappedObjectId(permission);
        assertEquals(1, results.size());
        assertEquals(objID, results.get(0));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getPoolsForObject(java.lang.String)}.
     */
    @Test
    public void getPoolsForObject() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        List<String> results = ACCESS_STORE.getPoolsForObject(objID);
        assertEquals(1, results.size());
        assertEquals(permission, results.get(0));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getDatabasePools()}.
     */
    @Test
    public void getDatabasePools() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        List<String> results = ACCESS_STORE.getDatabasePools();
        assertEquals(1, results.size());
        assertEquals(permission, results.get(0));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#existsRule(java.lang.String, java.lang.String)}.
     */
    @Test
    public void existsRule() {
        final String objID = "test";
        final String permission = "maytest";
        assertFalse(ACCESS_STORE.existsRule(objID, permission));
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        assertTrue(ACCESS_STORE.existsRule(objID, permission));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.jpa.access.MCRJPAAccessStore#getDistinctStringIDs()}.
     */
    @Test
    public void getDistinctStringIDs() {
        final String objID = "test";
        final String permission = "maytest";
        final String permission2 = "maytesttoo";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        addRuleMapping(objID, permission2, FALSE_RULE.getRid());
        startNewTransaction();
        List<String> results = ACCESS_STORE.getDistinctStringIDs();
        assertEquals(1, results.size());
        assertEquals(objID, results.get(0));
    }

}
