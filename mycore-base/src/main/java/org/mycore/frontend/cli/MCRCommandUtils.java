package org.mycore.frontend.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Utilities intended to reduce redundant code when writing variants of CLI commands.
 * 
 * @author Christoph Neidahl (OPNA2608)
 *
 */
public class MCRCommandUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Get a stream of MCRObjectIDs found for a type.
     * 
     * @param type
     *     The type to look in the store for.
     * @return A (parallel) stream with found IDs. (may be empty if none were found)
     * @throws MCRUsageException
     *     If no type was given or the type could not be found.
     *     Not thrown if type exists but has no values.
     */
    public static final Stream<String> getIdsForType(final String type) throws MCRUsageException {
        if (type == null || type.length() == 0) {
            throw new MCRUsageException("Type required to enumerate IDs!");
        }
        List<String> idList = MCRXMLMetadataManager.instance().listIDsOfType(type);
        if (idList.isEmpty()) {
            LOGGER.warn("No IDs found for type {}.", type);
        }
        return idList.stream();
    }

    /**
     * Get a stream of MCRObjectIDs found for a project &amp; type combination.
     * 
     * @param project
     *     The project to look in the store for.
     * @param type
     *     The type to look in the store for.
     * @return A (parallel) stream with found IDs. (may be empty if none were found)
     * @throws MCRUsageException
     *     If no project was given, no type was given or the base (${project}_${base}) could not be found.
     *     Not thrown if the base exists but has no values.
     */
    public static final Stream<String> getIdsForProjectAndType(final String project, final String type)
        throws MCRUsageException {
        if (project == null || project.length() == 0) {
            throw new MCRUsageException("Project required to enumerate IDs!");
        }
        if (type == null || type.length() == 0) {
            throw new MCRUsageException("Type required to enumerate IDs!");
        }
        return getIdsForBaseId(project + "_" + type);
    }

    /**
     * Get a stream of MCRObjectIDs found for a base.
     * 
     * @param base
     *     The base to look in the store for.
     * @return A (parallel) stream with found IDs. (may be empty if none were found)
     * @throws MCRUsageException
     *     If no base was given or the base could not be found.
     *     Not thrown if the base exists but has no values.
     */
    public static final Stream<String> getIdsForBaseId(final String base) throws MCRUsageException {
        if (MCRObjectID.getIDParts(base).length == 2) {
            throw new MCRUsageException("Base ID ({project}_{type}) required to enumerate IDs!");
        }
        List<String> idList = MCRXMLMetadataManager.instance().listIDsForBase(base);
        if (idList.isEmpty()) {
            LOGGER.warn("No IDs found for base {}.", base);
        }
        return idList.stream();
    }

    /**
     * Get a stream of MCRObjectIDs found in range between two IDs, incrementing/decrementing by 1.
     * 
     * @param startId
     *     The first ID to start iterating from.
     * @param endId
     *     The last ID to iterate towards.
     * @return A (parallel) stream with generated IDs that exist in the store. (may be empty if none exist)
     * @throws MCRUsageException
     *     If the supplied IDs are missing, invalid or have different base IDs, or if an error
     *     occurred while getting the store responsible for their base. The latter *may* occur if there does not
     *     yet exist a store for this base.
     *     Not thrown if the base exists but has no values.
     */
    public static final Stream<String> getIdsFromIdToId(final String startId, final String endId)
        throws MCRUsageException {
        if (startId == null || startId.length() == 0) {
            throw new MCRUsageException("Start-ID required to enumerate IDs!");
        }
        if (endId == null || endId.length() == 0) {
            throw new MCRUsageException("End-ID required to enumerate IDs!");
        }
        MCRObjectID from = MCRObjectID.getInstance(startId);
        MCRObjectID to = MCRObjectID.getInstance(endId);
        String fromBase = from.getBase();
        String toBase = to.getBase();
        if (!fromBase.equals(toBase)) {
            throw new MCRUsageException(
                startId + " and " + endId + " have different base IDs (" + fromBase + " and " + toBase
                    + "), same base required to enumerate IDs!");
        }
        
        int fromID = from.getNumberAsInteger();
        int toID = to.getNumberAsInteger();
        int lowerBound = fromID < toID ? fromID : toID;
        int upperBound = fromID < toID ? toID : fromID;
        List<String> idList = IntStream.rangeClosed(lowerBound, upperBound).boxed().parallel()
            .map(n -> MCRObjectID.formatID(fromBase, n))
            .filter(id -> MCRMetadataManager.exists(MCRObjectID.getInstance(id)))
            .collect(Collectors.toCollection(ArrayList::new));
        if (idList.isEmpty()) {
            LOGGER.warn("No IDs found in range [{} -> {}].", from, to);
        }
        return idList.stream();
    }
}
