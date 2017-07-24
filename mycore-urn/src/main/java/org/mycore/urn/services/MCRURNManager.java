/**
 *
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.urn.services;

import java.util.Hashtable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.urn.hibernate.MCRURN;

/**
 * Provides methods to create URNs (urn:nbn:de) and assign them to documents. A
 * URN (uniform resource name) is a special kind of persistent identifier. This
 * class handles URNs from the german subnamespace of NBN (national
 * bibliographic number), these URNs all start with urn:nbn:de:... More
 * information on persistent identifiers can be found at
 *
 * http://www.persistent-identifier.de/
 *
 * URNs are described by RFC 2141 and have the following syntax:
 * urn:[NID]:[SNID]-[NISS][Checksum] NID = namespace ID, in this implementation
 * always "nbn:de" SNID = subnamespace ID, a unique identifier for an
 * organization or public library that creates and assigns URNs within its
 * subnamespace NISS = namespace-specific string, a unique ID Checksum: all
 * nbn:de URNs end with one digit that is a checksum
 *
 * Example: urn:nbn:de:465-miless-20060622-213404-0017
 *
 * A MyCoRe systen can generate URNs for more than one subnamespace. There must
 * be one or more configurations that control the prefix (subnamespace) of
 * generated URNs and the algorithm used to build new NISS within that
 * subnamespace. Each configuration has a unique "subnamespace configuration ID"
 * in mycore.properties, and optional additional properties depending on the
 * implementation.
 *
 * MCR.URN.SubNamespace.[ConfigID].Prefix=[URNPrefix], for example
 * MCR.URN.SubNamespace.Essen.Prefix=urn.nbn.de:hbz:465-
 *
 * @author Frank Lützenkirchen
 * @author Robert Stephan
 */
@Deprecated
public class MCRURNManager {

    /** The MCRURNStore implementation to use */
    private static MCRURNStore store;

    private static final Logger LOGGER = LogManager.getLogger(MCRURNManager.class);

    static {
        try {

            store = (MCRURNStore) MCRConfiguration.instance().getSingleInstanceOf("MCR.Persistence.URN.Store.Class");
        } catch (Throwable t) {
            // TODO: handle exception
            LOGGER.error("Init error: ", t);
        }
    }

    /** A map from configID to MCRNissBuilder objects */
    private static Hashtable<String, MCRNISSBuilder> builders = new Hashtable<String, MCRNISSBuilder>();

    /**
     * Builds a URN with a custom, given NISS.
     *
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     * @param niss
     *            the custom NISS
     * @return the complete URN including prefix, NISS and calculated checksum
     */
    public static String buildURN(String configID, String niss) {
        String base = "MCR.URN.SubNamespace." + configID + ".";
        String prefix = MCRConfiguration.instance().getString(base + "Prefix");

        StringBuilder buffer = new StringBuilder(prefix);
        buffer.append(niss);
        return org.mycore.urn.services.MCRURN.create(buffer.toString()).toString();
    }

    /**
     * Builds a URN using a MCRNISSBuilder object.
     *
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     * @return the complete URN including prefix, niss and calculated checksum
     */
    public static synchronized String buildURN(String configID) throws MCRException {
        String base = "MCR.URN.SubNamespace." + configID + ".";

        MCRNISSBuilder builder = builders.get(configID);
        if (builder == null) {
            Object obj = MCRConfiguration.instance().getSingleInstanceOf(base + "NISSBuilder");
            builder = (MCRNISSBuilder) obj;
            builder.init(configID);
            builders.put(configID, builder);
        }

        String urn = null;
        String niss = null;
        String niss2 = null;
        do {
            niss = builder.buildNISS();
            if (niss.equals(niss2)) { // The niss doest'n change (missing counter?)
                throw new MCRException(
                    "Could not get unique NISS for URN. Possible reason: Missing counter in NISS Pattern.");
            }
            urn = buildURN(configID, niss);
            niss2 = niss;
        } while (isAssigned(urn));
        return urn;
    }

    /**
     * Returns true if the given URN has a valid structure and the checksum is
     * correct.
     */
    public static boolean isValid(String urn) {
        return org.mycore.urn.services.MCRURN.isValid(urn);
    }

    /** Returns true if the given urn is assigned to a document ID */
    public static boolean isAssigned(String urn) {
        return store.isAssigned(urn);
    }

    /** Assigns the given urn to the given document ID */
    public static void assignURN(String urn, String documentID) {
        store.assignURN(urn, documentID);
    }

    /**
     * @return true if the given object has an urn assigned
     * */
    public static boolean hasURNAssigned(String objId) {
        return store.hasURNAssigned(objId);
    }

    /**
     * Assigns the given urn to the given derivate ID
     * @param urn
     *      the urn to assign
     * @param derivateID
     *      the id of the derivate
     * @param path
     *      the path of the derivate in the internal filesystem
     * @param filename
     *      the filename
     */
    public static void assignURN(String urn, String derivateID, String path, String filename) {
        store.assignURN(urn, derivateID, path, filename);
    }

    public static void assignURN(String urn, MCRPath path) {
        assignURN(urn, path.getOwner(), path.getOwnerRelativePath());
    }

    /**
     * Assigns the given urn to the given derivate ID
     * @param urn
     *      the urn to assign
     * @param derivateID
     *      the id of the derivate
     * @param path
     *      the path of the derivate in the internal filesystem including file name
     */
    public static void assignURN(String urn, String derivateID, String path) {
        String[] pathParts = splitFilePath(path);
        store.assignURN(urn, derivateID, pathParts[0], pathParts[1]);
    }

    /**
     * Retrieves the URN that is assigned to the given document ID
     *
     * @return the urn, or null if no urn is assigned to this ID
     */
    public static String getURNforDocument(String documentID) {
        return store.getURNforDocument(documentID);
    }

    /**
     * @param filePath complete path to file
     * @return the URN for the given file if any
     */
    public static String getURNForFile(String derivateId, String filePath) {
        String[] pathParts = splitFilePath(filePath);
        return store.getURNForFile(derivateId, pathParts[0], pathParts[1]);
    }

    private static String[] splitFilePath(String filePath) {
        String path = filePath.charAt(0) == '/' ? filePath : "/" + filePath;
        int i = path.lastIndexOf("/") + 1;
        String file = path.substring(i);
        String pathDb = path.substring(0, i);
        String[] pathParts = new String[] { pathDb, file };
        return pathParts;
    }

    public static String getURNForFile(String derivateId, String path, String fileName) {
        return store.getURNForFile(derivateId, path, fileName);
    }

    public static String getURNForPath(MCRPath path) {
        return getURNForFile(path.getOwner(), path.getOwnerRelativePath());
    }

    /**
     * Retrieves the document ID that is assigned to the given urn
     *
     * @return the ID, or null if no ID is assigned to this urn
     */
    public static String getDocumentIDforURN(String urn) {
        return store.getDocumentIDforURN(urn);
    }

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     */
    public static void removeURN(String urn) {
        store.removeURN(urn);
    }

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     */
    public static void removeURNByObjectID(String objID) {
        store.removeURNByObjectID(objID);
    }

    /**
     * Create and Assign a new URN to the given Document
     * Ensure that new created URNs do not allready exist in URN store
     * @param documentID a MCRID
     * @param configID - the configurationID of the URN Builder
     * @return the URN
     */
    public static synchronized String buildAndAssignURN(String documentID, String configID) {
        if (hasURNAssigned(documentID)) {
            String urn = getURNforDocument(documentID);
            LOGGER.warn("Returning the already assign URN: " + urn + " for document " + documentID + ".");
            return urn;
        }
        String urn = buildURN(configID);
        assignURN(urn, documentID);
        return urn;
    }

    /**
     * @return the count of urn matching the given 'registered' attribute
     */
    public static long getCount(boolean registered) {
        return store.getCount(registered);
    }

    public static List<MCRURN> get(boolean registered, int start, int rows) {
        return store.get(registered, start, rows);
    }

    public static void update(MCRURN urn) {
        store.update(urn);
    }

    /**
     * Get all URN for the given object id.
     */
    public static List<MCRURN> get(MCRObjectID id) {
        return store.get(id);
    }

    /**
     * @return a {@link List} of {@link MCRURN} where path and file name are just blanks or null;
     */
    public static List<MCRURN> getBaseURN(boolean registered, boolean dfg, int start, int rows) {
        return store.getBaseURN(registered, dfg, start, rows);
    }
}
