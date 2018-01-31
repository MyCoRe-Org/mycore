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

package org.mycore.pi.urn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRFileCollectingFileVisitor;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Service for assigning granular URNs to Derivate. You can call it with a Derivate-ID and it will assign a Base-URN for
 * the Derivate and granular URNs for every file in the Derivate (except IgnoreFileNames). If you then add a file to
 * Derivate you can call with Derivate-ID and additional path of the file. E.g. mir_derivate_00000060 and /image1.jpg
 * <p> <b>Inscriber is ignored with this {@link MCRPIRegistrationService}</b> </p> Configuration Parameter(s): <dl>
 * <dt>IgnoreFileNames</dt> <dd>Comma seperated list of regex file which should not have a urn assigned. Default:
 * mets\\.xml</dd> </dl>
 */
public class MCRURNGranularOAIRegistrationService extends MCRPIRegistrationService<MCRDNBURN> {

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRURNGranularOAIRegistrationService(String registrationServiceID) {
        super(registrationServiceID, MCRDNBURN.TYPE);
    }

    @Override
    public MCRDNBURN register(MCRBase obj, String additional, boolean updateObject)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, additional);

        MCRObjectDerivate derivate = ((MCRDerivate) obj).getDerivate();
        MCRDNBURN newURN;

        if (additional.equals("")) {
            /* Multiple URN for entire Derivate...  */
            newURN = registerURNsDerivate(obj, additional, derivate);
        } else {
            /* Single URN to one File... */
            newURN = registerSingleURN(obj, additional, derivate);
        }

        try {
            MCRMetadataManager.update(obj);
        } catch (Exception e) {
            throw new MCRPersistentIdentifierException("Error while updating derivate " + obj.getId(), e);
        }

        return newURN;

    }

    private MCRDNBURN registerSingleURN(MCRBase obj, String additional, MCRObjectDerivate derivate)
        throws MCRPersistentIdentifierException {
        MCRDNBURN newURN;
        LOGGER.info("Add single urn to {} / {}", obj.getId(), additional);

        Session session = MCRHIBConnection.instance().getSession();
        MCRPath filePath;
        if (!Files.exists(filePath = MCRPath.getPath(obj.getId().toString(), additional))) {
            throw new MCRPersistentIdentifierException("Invalid path : " + additional);
        }

        int count = Math.toIntExact(derivate.getFileMetadata().stream().filter(file -> file.getUrn() != null).count());
        MCRDNBURN dnbURN = newURN = (MCRDNBURN) MCRPersistentIdentifierManager.getInstance().get(derivate.getURN())
            .findFirst().get();

        String setID = obj.getId().getNumberAsString();
        MCRDNBURN urntoAssign = dnbURN.toGranular(setID, count + 1, count + 1);
        derivate.getOrCreateFileMetadata(filePath, urntoAssign.asString()).setUrn(urntoAssign.asString());
        MCRPI databaseEntry = new MCRPI(urntoAssign.asString(), getType(), obj.getId().toString(), additional,
            this.getRegistrationServiceID(), new Date());
        session.save(databaseEntry);
        return newURN;
    }

    private MCRDNBURN registerURNsDerivate(MCRBase obj, String additional, MCRObjectDerivate derivate)
        throws MCRPersistentIdentifierException {
        LOGGER.info("Add URNs to all files of {}", obj.getId());

        Session session = MCRHIBConnection.instance().getSession();

        Path path = MCRPath.getPath(obj.getId().toString(), "/");
        MCRFileCollectingFileVisitor<Path> collectingFileVisitor = new MCRFileCollectingFileVisitor<>();

        try {
            Files.walkFileTree(path, collectingFileVisitor);
        } catch (IOException e) {
            throw new MCRPersistentIdentifierException("Could not walk derivate file tree!", e);
        }

        List<String> ignoreFileNamesList = getIgnoreFileList();

        List<Predicate<String>> predicateList = ignoreFileNamesList
            .stream()
            .map(Pattern::compile)
            .map(Pattern::asPredicate)
            .collect(Collectors.toList());

        List<MCRPath> pathList = collectingFileVisitor
            .getPaths()
            .stream()
            .filter(file -> predicateList.stream()
                .noneMatch(p -> p.test(file.toString().split(":")[1])))
            .map(p -> (MCRPath) p)
            .sorted()
            .collect(Collectors.toList());

        MCRDNBURN newURN = getNewIdentifier(obj.getId(), additional);
        String setID = obj.getId().getNumberAsString();

        for (int pathListIndex = 0; pathListIndex < pathList.size(); pathListIndex++) {
            MCRDNBURN subURN = newURN.toGranular(setID, pathListIndex + 1, pathList.size());
            derivate.getOrCreateFileMetadata(pathList.get(pathListIndex), subURN.asString()).setUrn(subURN.asString());
            MCRPI databaseEntry = new MCRPI(subURN.asString(), getType(), obj.getId().toString(),
                pathList.get(pathListIndex).getOwnerRelativePath(),
                this.getRegistrationServiceID(), null);
            session.save(databaseEntry);
        }

        derivate.setURN(newURN.asString());
        MCRPI databaseEntry = new MCRPI(newURN.asString(), getType(), obj.getId().toString(), "",
            this.getRegistrationServiceID(), new Date());
        session.save(databaseEntry);
        return newURN;
    }


    private List<String> getIgnoreFileList() {
        List<String> ignoreFileNamesList = new ArrayList<>();
        String ignoreFileNames = getProperties().get("IgnoreFileNames");
        if (ignoreFileNames != null) {
            ignoreFileNamesList.addAll(Arrays.asList(ignoreFileNames.split(",")));
        } else {
            ignoreFileNamesList.add("mets\\.xml"); // default value
        }
        return ignoreFileNamesList;
    }

    @Override
    protected MCRDNBURN registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        // not used in this impl
        return null;
    }

    @Override
    protected void delete(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Delete is not supported for " + getType());
    }

    @Override
    protected void update(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        //TODO: improve API, don't override method to do nothing
        LOGGER.info("No update in this implementation");
    }
}
