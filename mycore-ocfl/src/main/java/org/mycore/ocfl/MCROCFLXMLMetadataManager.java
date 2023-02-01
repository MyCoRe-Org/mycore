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

package org.mycore.ocfl;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.util.MCROCFLMetadataVersion;

import edu.wisc.library.ocfl.api.OcflRepository;

/**
 * @deprecated use {@link org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager MCROCFLXMLMetadataManager}
 */
@Deprecated(forRemoval = true)
public class MCROCFLXMLMetadataManager extends metadata.MCROCFLXMLMetadataManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEP_WARN
        = "\u001B[93m" + "Usage of the toplevel ocfl classes is deprecated and will be removed in future releases, " +
            "please use 'org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager' instead." + "\u001B[0m";

    public MCROCFLXMLMetadataManager() {
        LOGGER.warn(DEP_WARN);
    }

    @Override
    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified, String user)
        throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.create(mcrid, xml, lastModified, user);
    }

    @Override
    public void delete(MCRObjectID mcrid, Date date, String user) throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.delete(mcrid, date, user);
    }

    @Override
    public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        return super.exists(mcrid);
    }

    @Override
    public int getHighestStoredID(String project, String type) {
        LOGGER.warn(DEP_WARN);
        return super.getHighestStoredID(project, type);
    }

    @Override
    public long getLastModified(String ocflObjectId) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.getLastModified(ocflObjectId);
    }

    @Override
    public ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit) {
        LOGGER.warn(DEP_WARN);
        return super.getLastModifiedHandle(id, expire, unit);
    }

    @Override
    public Collection<String> getObjectBaseIds() {
        LOGGER.warn(DEP_WARN);
        return super.getObjectBaseIds();
    }

    @Override
    public Collection<String> getObjectTypes() {
        LOGGER.warn(DEP_WARN);
        return super.getObjectTypes();
    }

    @Override
    public OcflRepository getRepository() {
        LOGGER.warn(DEP_WARN);
        return super.getRepository();
    }

    @Override
    public IntStream getStoredIDs(String project, String type) throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        return super.getStoredIDs(project, type);
    }

    @Override
    public List<String> listIDs() {
        LOGGER.warn(DEP_WARN);
        return super.listIDs();
    }

    @Override
    public List<String> listIDsForBase(String base) {
        LOGGER.warn(DEP_WARN);
        return super.listIDsForBase(base);
    }

    @Override
    public List<String> listIDsOfType(String type) {
        LOGGER.warn(DEP_WARN);
        return super.listIDsOfType(type);
    }

    @Override
    public List<MCROCFLMetadataVersion> listRevisions(MCRObjectID id) {
        LOGGER.warn(DEP_WARN);
        return super.listRevisions(id);
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.retrieveContent(mcrid);
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid, String revision) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.retrieveContent(mcrid, revision);
    }

    @Override
    public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.retrieveObjectDates(ids);
    }

    @Override
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.update(mcrid, xml, lastModified);
    }
    @Override
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified, String user)
        throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.update(mcrid, xml, lastModified, user);
    }

    @Override
    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.create(mcrid, xml, lastModified);
    }

    @Override
    public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
        LOGGER.warn(DEP_WARN);
        super.delete(mcrid);
    }

}
