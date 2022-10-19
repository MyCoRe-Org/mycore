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
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLClassificationManager;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.exception.ObjectOutOfSyncException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * OCFL File Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLClassificationManager implements MCRXMLClassificationManager {

    public static final String MESSAGE_CREATED = "Created";

    public static final String MESSAGE_UPDATED = "Updated";

    public static final String MESSAGE_DELETED = "Deleted";

    private static final String ROOT_FOLDER = "classification/";

    private OcflRepository repository;

    protected static final Map<String, Character> MESSAGE_TYPE_MAPPING = Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCROCFLMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCROCFLMetadataVersion.DELETED));

    protected static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    /**
     * Initializes the ClassificationManager
     */
    public MCROCFLXMLClassificationManager() {

    }

    /**
    * Initializes the ClassificationManager with the given repositoryKey.
    * @param repositoryKey the ID for the repository to be used
    */
    public MCROCFLXMLClassificationManager(String repositoryKey) {
        initOCFLRepository(repositoryKey);
        
    }
    
    /**
     * initializes the OCFL repository with the given repositoryKey
     * 
     * @param repositoryKey
     */
    @MCRProperty(name = "OCFL.Repository")
    public void initOCFLRepository(String repositoryKey) {
        repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }

    public OcflRepository getRepository() {
        return repository;
    }

    public void create(MCRCategoryID mcrCg, MCRContent xml) throws IOException {
        fileUpdate(mcrCg, xml, MESSAGE_CREATED);
    }

    public void update(MCRCategoryID mcrCg, MCRContent xml) throws IOException {
        fileUpdate(mcrCg, xml, MESSAGE_UPDATED);
    }

    void fileUpdate(MCRCategoryID mcrCg, MCRContent xml, String messageOpt) throws IOException {
        String ocflObjectID = getOCFLObjectID(mcrCg);
        String message = messageOpt; // PMD Fix - AvoidReassigningParameters
        if (Objects.isNull(message)) {
            message = MESSAGE_UPDATED;
        }

        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(message, new Date());
            getRepository().updateObject(ObjectVersionId.head(ocflObjectID), versionInfo,
                updater -> updater.writeFile(objectAsStream, buildFilePath(mcrCg), OcflOption.OVERWRITE));
        }
    }

    public void delete(MCRCategoryID mcrid) throws IOException {
        String ocflObjectID = getOCFLObjectID(mcrid);
        VersionInfo versionInfo = buildVersionInfo(MESSAGE_DELETED, new Date());
        try {
            getRepository().updateObject(ObjectVersionId.head(ocflObjectID), versionInfo,
                updater -> updater.removeFile(buildFilePath(mcrid)));
        } catch (NotFoundException | ObjectOutOfSyncException e) {
            throw new IOException(e);
        }
    }

    /**
     * Load a Classification from the OCFL Store.
     * @param mcrid ID of the Category
     * @param revision Revision of the Category or <code>null</code> for HEAD
     * @return Content of the Classification
     */
    @Override
    public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) throws IOException {
        String ocflObjectID = getOCFLObjectID(mcrid);
        OcflRepository repo = getRepository();
        ObjectVersionId vId = revision != null ? ObjectVersionId.version(ocflObjectID, revision)
            : ObjectVersionId.head(ocflObjectID);

        try {
            repo.getObject(vId);
        } catch (NotFoundException e) {
            throw new IOException("Object '" + ocflObjectID + "' could not be found", e);
        }

        if (convertMessageToType(repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new IOException("Cannot read already deleted object '" + ocflObjectID + "'");
        }

        try (InputStream storedContentStream = repo.getObject(vId).getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            if (revision != null) {
                xml.getRootElement().setAttribute("rev", revision);
            }
            return new MCRJDOMContent(xml);
        } catch (JDOMException | SAXException e) {
            throw new IOException("Can not parse XML from OCFL-Store", e);
        }
    }

    protected String getOCFLObjectID(MCRCategoryID mcrid) {
        return MCROCFLObjectIDPrefixHelper.CLASSIFICATION + mcrid.getRootID();
    }

    /**
     * Build file path from ID, <em>use for root classifications only!</em>
     * @param mcrid The ID to the Classification
     * @return The Path to the File.
     * @throws MCRUsageException if the Category is not a root classification
     */
    protected String buildFilePath(MCRCategoryID mcrid) {
        if (!mcrid.isRootID()) {
            throw new IllegalArgumentException("Only root categories are allowed: " + mcrid);
        }
        return ROOT_FOLDER + mcrid + ".xml";
    }

    protected VersionInfo buildVersionInfo(String message, Date versionDate) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setMessage(message);
        versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
        return versionInfo;
    }

}
