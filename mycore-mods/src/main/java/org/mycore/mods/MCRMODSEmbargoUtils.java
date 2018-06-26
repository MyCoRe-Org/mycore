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
package org.mycore.mods;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRCreatorCache;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRMODSEmbargoUtils {

    public static final String POOLPRIVILEGE_EMBARGO = "embargo";

    private static final Logger LOGGER = LogManager.getLogger(MCRMODSEmbargoUtils.class);

    private static final String EMPTY_VALUE = "";

    private static final int CAPACITY = 10000;

    private static MCRCache<MCRObjectID, String> embargoCache = new MCRCache<>(CAPACITY, "MODS embargo filter cache");

    public static String getCachedEmbargo(final MCRObjectID objectId) {
        MCRCache.ModifiedHandle modifiedHandle = MCRXMLMetadataManager.instance().getLastModifiedHandle(objectId, 10,
            TimeUnit.MINUTES);
        String embargo = null;
        try {
            embargo = embargoCache.getIfUpToDate(objectId, modifiedHandle);
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified timestamp of object {}", objectId);
        }
        if (embargo != null) {
            return embargo == EMPTY_VALUE ? null : embargo;
        }
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(objectId));
        embargo = modsWrapper.getElementValue("mods:accessCondition[@type='embargo']");
        embargoCache.put(objectId, embargo != null ? embargo : EMPTY_VALUE);
        return embargo;
    }

    /**
     * Returns the embargo or <code>null</code> if none is set or is allowed to read.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return the embargo or <code>null</code>
     */
    public static String getEmbargo(final String objectId) {
        return getEmbargo(MCRObjectID.getInstance(objectId));
    }

    /**
     * Returns the embargo or <code>null</code> if none is set or is allowed to read.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return the embargo or <code>null</code>
     */
    public static String getEmbargo(final MCRObjectID objectId) {
        String embargo = getCachedEmbargo(objectId);

        if (embargo != null && !embargo.isEmpty() && isAfterToday(embargo)) {
            return embargo;
        }
        return null;
    }

    public static String getEmbargo(final MCRObject object) {
        final MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
        final String embargo = modsWrapper.getElementValue("mods:accessCondition[@type='embargo']");

        if (embargo != null && !embargo.isEmpty() && isAfterToday(embargo)) {
            return embargo;
        }
        return null;
    }

    public static Optional<LocalDate> getEmbargoDate(final String objectID) {
        return parseEmbargo(getEmbargo(objectID));
    }

    public static Optional<LocalDate> getEmbargoDate(final MCRObjectID object) {
        return parseEmbargo(getEmbargo(object));
    }

    public static Optional<LocalDate> getEmbargoDate(final MCRObject object) {
        return parseEmbargo(getEmbargo(object));
    }

    private static Optional<LocalDate> parseEmbargo(final String embargoDate) {
        final MCRISO8601Date isoED = new MCRISO8601Date(embargoDate);
        return Optional.ofNullable(LocalDate.from(isoED.getDt()));
    }

    public static boolean isAfterToday(final String embargoDate) {
        try {
            final Optional<LocalDate> ed = parseEmbargo(embargoDate);
            final LocalDate now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
            final boolean bool = ed.map(ded -> ded.isAfter(now)).orElseGet(() -> false);
            return bool;
        } catch (DateTimeException ex) {
            return embargoDate.compareTo(MCRISO8601Date.now().getISOString()) > 0;
        }
    }

    public static boolean isCurrentUserCreator(final MCRObjectID objectId) {
        try {
            final String creator = MCRCreatorCache.getCreator(objectId);
            return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID().equals(creator);
        } catch (ExecutionException e) {
            LOGGER.error("Error while getting creator information.", e);
            return false;
        }
    }
}
