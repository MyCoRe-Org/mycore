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

package org.mycore.test;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * The {@link MCRXMLMetadataManager} that {@link MCRTestExtension} configures by default: every operation fails.
 * <p>
 * A metadata store is not free of side effects on the JVM it runs in. It registers itself in the JVM-wide
 * {@link org.mycore.datamodel.ifs2.MCRStoreCenter} and it writes to a directory that, without
 * {@link MCRMetadataExtension}, is shared by every test in the same fork. A test that uses metadata without
 * declaring that extension therefore affects unrelated test classes, and typically only for some test execution
 * orders - the kind of failure that surfaces on one CI agent and is not reproducible anywhere else.
 * <p>
 * Failing here makes that a deterministic error in the test that causes it, rather than an error in whichever
 * test happens to run next.
 *
 * @see MCRMetadataExtension
 */
public class MCRAlwaysFailingXMLMetadataManager implements MCRXMLMetadataManager {

    @Override
    public void reload() {
        throw fail("reload");
    }

    @Override
    public void verifyStore(String base) {
        throw fail("verifyStore");
    }

    @Override
    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        throw fail("create");
    }

    @Override
    public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
        throw fail("delete");
    }

    @Override
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        throw fail("update");
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid) {
        throw fail("retrieveContent");
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid, String revision) {
        throw fail("retrieveContent");
    }

    @Override
    public List<? extends MCRAbstractMetadataVersion<?>> listRevisions(MCRObjectID id) {
        throw fail("listRevisions");
    }

    @Override
    public int getHighestStoredID(String project, String type) {
        throw fail("getHighestStoredID");
    }

    @Override
    public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
        throw fail("exists");
    }

    @Override
    public List<String> listIDsForBase(String base) {
        throw fail("listIDsForBase");
    }

    @Override
    public List<String> listIDsOfType(String type) {
        throw fail("listIDsOfType");
    }

    @Override
    public List<String> listIDs() {
        throw fail("listIDs");
    }

    @Override
    public Collection<String> getObjectTypes() {
        throw fail("getObjectTypes");
    }

    @Override
    public Collection<String> getObjectBaseIds() {
        throw fail("getObjectBaseIds");
    }

    @Override
    public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) {
        throw fail("retrieveObjectDates");
    }

    @Override
    public long getLastModified(MCRObjectID id) {
        throw fail("getLastModified");
    }

    @Override
    public MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit) {
        throw fail("getLastModifiedHandle");
    }

    private static MCRException fail(String operation) {
        return new MCRException("This test used the metadata store (" + operation + "), but it is not set up for it."
            + " Add @ExtendWith(" + MCRMetadataExtension.class.getSimpleName() + ".class) to the test class, so that"
            + " it gets a temporary store of its own that is cleaned up afterwards. Without it, the test would"
            + " write to a directory shared with every other test of the same fork and would leave a store behind"
            + " in the JVM-wide MCRStoreCenter.");
    }

}
