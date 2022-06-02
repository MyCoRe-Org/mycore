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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLClassificationManager;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * OCFL File Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLClassificationManager implements MCRXMLClassificationManager {

    protected static final String CLASSIFICATION_PREFIX = "mcrclass:";

    private static final boolean INC_CLSDIR = MCRConfiguration2.getBoolean(CLASSIFICATION_PREFIX).orElse(false);

    public static final String MESSAGE_CREATED = "Created";

    public static final String MESSAGE_UPDATED = "Updated";

    public static final String MESSAGE_DELETED = "Deleted";

    private String rootFolder = INC_CLSDIR ? "classification/" : "";

    @MCRProperty(name = "Repository", required = true)
    public String repositoryKey;

    protected static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCROCFLMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCROCFLMetadataVersion.DELETED)));

    protected static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    protected OcflRepository getRepository() throws ClassCastException {
        return MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }

    public void create(MCRCategory mcrCg, MCRContent xml) {
        fileUpdate(mcrCg, xml, MESSAGE_CREATED);
    }

    public void update(MCRCategory mcrCg, MCRContent xml) {
        fileUpdate(mcrCg, xml, MESSAGE_UPDATED);
    }

    void fileUpdate(MCRCategory mcrCg, MCRContent xml, String messageOpt) {
        
        String objName = getName(mcrCg.getId());
        Date lastModified = new Date(MCRCategoryDAOFactory.getInstance().getLastModified(mcrCg.getId().getID()));
        String message = messageOpt; // PMD Fix - AvoidReassigningParameters
        if(Objects.isNull(message)) {
            message = MESSAGE_UPDATED;
        }

        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(message, lastModified);
            getRepository().updateObject(ObjectVersionId.head(objName), versionInfo, updater -> {
                updater.writeFile(objectAsStream, buildFilePath(mcrCg), OcflOption.OVERWRITE);
            });
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        } catch (IllegalArgumentException e) {
            // Issue seems to be with the OCFL Library trying to find a file it deleted itself
            // LOGGER.error("Something has gone Wrong!", e);
        }

    }

    public void fileDelete(MCRCategoryID mcrid) {
        String objName = getName(mcrid);
        Date lastModified = new Date(MCRCategoryDAOFactory.getInstance().getLastModified(mcrid.getRootID()));
        VersionInfo versionInfo = buildVersionInfo(MESSAGE_DELETED, lastModified);
        getRepository().updateObject(ObjectVersionId.head(objName), versionInfo, updater -> {
            updater.removeFile(buildFilePath(mcrid));
        });
    }

    /**
     * Load a Classification from the OCFL Store.
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return Content of the Classification
     */
    public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) {
        String objName = getName(mcrid);
        OcflRepository repo = getRepository();
        ObjectVersionId vId = revision != null ? ObjectVersionId.version(objName, revision)
            : ObjectVersionId.head(objName);

        try {
            repo.getObject(vId);
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }

        if (convertMessageToType(repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }

        try (InputStream storedContentStream = repo.getObject(vId).getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            if (revision != null) {
                xml.getRootElement().setAttribute("rev", revision);
            }
            return new MCRJDOMContent(xml);
        } catch (JDOMException | SAXException | IOException e) {
            throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        }
    }

    protected String getName(MCRCategoryID mcrid) {
        return CLASSIFICATION_PREFIX + mcrid.getRootID();
    }

    /**
     * Build file path from ID, use for classifications only!
     * <p><b>Use {@link #buildFilePath(MCRCategory)} when possible instead</b></p>
     * @param mcrid The ID to the Classification
     * @return The Path to the File.
     */
    protected String buildFilePath(MCRCategoryID mcrid) {
        if (mcrid.isRootID()) {
            return rootFolder + mcrid.toString() + ".xml";
        } else {
            return rootFolder + mcrid.getRootID() + '/' + mcrid.toString() + ".xml";
        }
    }

    protected String buildFilePath(MCRCategory mcrCg) {
        StringBuilder builder = new StringBuilder(rootFolder);
        if (mcrCg.isClassification()) {
            builder.append(mcrCg.getId()).append(".xml");
        } else if (mcrCg.isCategory()) {
            ArrayList<String> list = new ArrayList<>();
            list.add(".xml");
            MCRCategory cg = mcrCg;
            int level = mcrCg.getLevel();
            while (level > 0) {
                MCRCategory cgt = cg;
                list.add(cg.getId().toString());
                list.add("/");
                cg = cgt.getParent();
                level = cg.getLevel();
            }
            list.add(cg.getId().toString());
            for (int i = list.size() - 1; i >= 0; i--) {
                builder.append(list.get(i));
            }
        } else {
            throw new MCRException("The MCRClass is of Invalid Type, it must be either a Class or Category");
        }
        return builder.toString();
    }

    protected VersionInfo buildVersionInfo(String message, Date versionDate) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setMessage(message);
        versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
        return versionInfo;
    }

}
