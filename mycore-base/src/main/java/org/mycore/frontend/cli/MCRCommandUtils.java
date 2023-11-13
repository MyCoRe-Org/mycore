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

package org.mycore.frontend.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRUsageException;
import org.mycore.common.xml.MCRURIResolver;
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
    public static Stream<String> getIdsForType(final String type) throws MCRUsageException {
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
    public static Stream<String> getIdsForProjectAndType(final String project, final String type)
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
    public static Stream<String> getIdsForBaseId(final String base) throws MCRUsageException {
        if (MCRObjectID.getIDParts(base).length != 2) {
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
    public static Stream<String> getIdsFromIdToId(final String startId, final String endId)
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

    /**
     * This method search for the stylesheet <em>style</em>.xsl and builds a transformer. A fallback is
     * used if no stylesheet is given or the stylesheet couldn't be resolved.
     *
     * @param style
     *            the name of the style to be used when resolving the stylesheet.
     * @param cache
     *            The transformer cache to be used.
     * @return the transformer
     */
    public static Transformer getTransformer(String style, String defaultStyle, Map<String, Transformer> cache) {
        if (cache.containsKey(style)) {
            return cache.get(style);
        }

        Element element = MCRURIResolver.instance().resolve("resource:" + style);
        if(element == null) {
            LOGGER.warn("Could not load transformer from resource {}, trying default {}.", style, defaultStyle);
            element = MCRURIResolver.instance().resolve("resource:" + defaultStyle);
        }
        try {
            if (element != null) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformerFactory.setURIResolver(MCRURIResolver.instance());
                Transformer transformer = transformerFactory.newTransformer(new JDOMSource(element));
                cache.put(style, transformer);
                LOGGER.info("Loaded transformer from resource {}.", style);
                return transformer;
            } else {
                LOGGER.warn("Could not load transformer from resource {}.", style);
            }
        } catch (Exception e) {
            LOGGER.warn("Error while loading transformer from resource " + style + ".", e);
        }
        return null;
    }
}
