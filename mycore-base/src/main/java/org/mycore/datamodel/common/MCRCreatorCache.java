/*
 * $Id$ 
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
package org.mycore.datamodel.common;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRCreatorCache {

    private static final Logger LOGGER = LogManager.getLogger(MCRCreatorCache.class);

    private static final long CACHE_SIZE = 5000;

    private static LoadingCache<MCRObjectID, String> CACHE = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE).build(new CacheLoader<MCRObjectID, String>() {
            @Override
            public String load(final MCRObjectID objectId) throws Exception {
                return Optional.ofNullable(MCRMetadataManager.retrieveMCRObject(objectId).getService()).map(os -> {
                    if (os.isFlagTypeSet("createdby")) {
                        final String creator = os.getFlags("createdby").get(0);
                        LOGGER.info("Found creator " + creator + " of " + objectId);
                        return creator;
                    }
                    LOGGER.info("Try to get creator information of " + objectId + " from svn history.");
                    return null;
                }).orElseGet(() -> {
                    try {
                        return Optional.ofNullable(MCRXMLMetadataManager.instance().listRevisions(objectId))
                            .map(versions -> {
                                return versions.stream()
                                    .sorted(Comparator.comparingLong(MCRMetadataVersion::getRevision)
                                        .reversed())
                                    .filter(v -> v.getType() == MCRMetadataVersion.CREATED).findFirst()
                                    .map(version -> {
                                        LOGGER.info(
                                            "Found creator " + version.getUser() + " in revision "
                                                + version.getRevision()
                                                + " of " + objectId);
                                        return version.getUser();
                                    }).orElseGet(() -> {
                                        LOGGER.info(
                                            "Could not get creator information of " + objectId + ".");
                                        return null;
                                    });
                            }).orElseGet(() -> {
                                LOGGER.info("Could not get creator information.");
                                return null;
                            });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });

    /**
     * Returns the creator by given {@link MCRObjectID}.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return the creator of the object
     * @throws ExecutionException is thrown if any other exception occurs
     */
    public static String getCreator(final MCRObjectID objectId) throws ExecutionException {
        return CACHE.get(objectId);
    }

    /**
     * Returns the creator by given {@link MCRObjectID}.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return the creator of the object
     * @throws ExecutionException is thrown if any other exception occurs
     */
    public static String getCreator(final String objectId) throws ExecutionException {
        return CACHE.get(MCRObjectID.getInstance(objectId));
    }

    /**
     * Discard the cached creator for given {@link MCRObjectID}.
     *  
     * @param objectId the {@link MCRObjectID}
     */
    public static void invalidate(final MCRObjectID objectId) {
        CACHE.invalidate(objectId);
    }

    /**
     * Discard the cached creator for given {@link MCRObjectID}.
     * 
     * @param objectId the {@link MCRObjectID}
     */
    public static void invalidate(final String objectId) {
        CACHE.invalidate(MCRObjectID.getInstance(objectId));
    }
}
