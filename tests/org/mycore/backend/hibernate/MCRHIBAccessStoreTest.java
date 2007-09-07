/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRHibTestCase;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRHIBAccessStoreTest extends MCRHibTestCase {

    private static final MCRACCESSRULE TRUE_RULE = getTrueRule();

    private static final MCRACCESSRULE FALSE_RULE = getFalseRule();

    private static MCRHIBAccessStore ACCESS_STORE;

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if (ACCESS_STORE == null) {
            ACCESS_STORE = new MCRHIBAccessStore();
        }
        sessionFactory.getCurrentSession().save(TRUE_RULE);
        sessionFactory.getCurrentSession().save(FALSE_RULE);
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#createAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    public void testCreateAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        assertNotNull(sessionFactory.getCurrentSession().get(MCRACCESS.class, new MCRACCESSPK(permission, objID)));
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
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getRuleID(String, String)}.
     */
    public void testGetRuleID() {
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
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#deleteAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    public void testDeleteAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        ACCESS_STORE.deleteAccessDefinition(ruleMapping);
        startNewTransaction();
        assertNull(sessionFactory.getCurrentSession().get(MCRACCESS.class, new MCRACCESSPK(permission, objID)));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#updateAccessDefinition(org.mycore.access.mcrimpl.MCRRuleMapping)}.
     */
    public void testUpdateAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        ruleMapping.setRuleId(FALSE_RULE.getRid());
        ACCESS_STORE.updateAccessDefinition(ruleMapping);
        startNewTransaction();
        MCRACCESS access = (MCRACCESS) sessionFactory.getCurrentSession().get(MCRACCESS.class, new MCRACCESSPK(permission, objID));
        assertEquals(FALSE_RULE, access.getRule());
    }

    /**
     * Test method for
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getAccessDefinition(java.lang.String, java.lang.String)}.
     */
    public void testGetAccessDefinition() {
        final String objID = "test";
        final String permission = "maytest";
        MCRRuleMapping ruleMapping = addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        MCRRuleMapping ruleMapping2 = ACCESS_STORE.getAccessDefinition(permission, objID);
        // We will remove milliseconds as they don't need to be saved
        assertEquals((long) Math.floor(ruleMapping.getCreationdate().getTime() / 1000), ruleMapping2.getCreationdate().getTime() / 1000);
        assertEquals(ruleMapping.getCreator(), ruleMapping2.getCreator());
        assertEquals(ruleMapping.getObjId(), ruleMapping2.getObjId());
        assertEquals(ruleMapping.getPool(), ruleMapping2.getPool());
        assertEquals(ruleMapping.getRuleId(), ruleMapping2.getRuleId());
    }

    /**
     * Test method for
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getMappedObjectId(java.lang.String)}.
     */
    @SuppressWarnings("unchecked")
    public void testGetMappedObjectId() {
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
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getPoolsForObject(java.lang.String)}.
     */
    @SuppressWarnings("unchecked")
    public void testGetPoolsForObject() {
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
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getDatabasePools()}.
     */
    @SuppressWarnings("unchecked")
    public void testGetDatabasePools() {
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
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#existsRule(java.lang.String, java.lang.String)}.
     */
    public void testExistsRule() {
        final String objID = "test";
        final String permission = "maytest";
        assertFalse(ACCESS_STORE.existsRule(objID, permission));
        addRuleMapping(objID, permission, TRUE_RULE.getRid());
        startNewTransaction();
        assertTrue(ACCESS_STORE.existsRule(objID, permission));
    }

    /**
     * Test method for
     * {@link org.mycore.backend.hibernate.MCRHIBAccessStore#getDistinctStringIDs()}.
     */
    @SuppressWarnings("unchecked")
    public void testGetDistinctStringIDs() {
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
