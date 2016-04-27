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
package org.mycore.mods;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRMODSEmbargoUtils {

    private static final String POOLPRIVILEGE_EMBARGO = "embargo";

    private static final int CAPACITY = 10000;

    private static final Logger LOGGER = Logger.getLogger(MCRMODSEmbargoUtils.class);

    private static final String EMPTY_VALUE = "";

    private static MCRCache<MCRObjectID, String> embargoCache = new MCRCache<>(CAPACITY, "MODS embargo filter cache");

    /**
     * Checks if current user is allowed to read the given {@link MCRObjectID}.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return <code>true</code> is allowed to read
     */
    public static boolean isReadAllowed(final MCRObjectID objectId) {
        if (objectId == null || !"mods".equals(objectId.getTypeId())) {
            return true;
        }

        return MCRAccessManager.checkPermission(objectId, MCRAccessManager.PERMISSION_READ)
                && MCRAccessManager.checkPermission(POOLPRIVILEGE_EMBARGO);
    }

    /**
     * Returns the embargo or <code>null</code> if none is set or is allowed to read.
     * 
     * @param objectId the {@link MCRObjectID}
     * @return the embargo or <code>null</code>
     */
    public static String getEmbargo(final MCRObjectID objectId) {
        if (!isReadAllowed(objectId)) {
            String embargo = getCachedEmbargo(objectId);

            if (embargo != null && !embargo.isEmpty() && isAfterToday(embargo)) {
                return embargo;
            }
        }

        return null;
    }

    private static String getCachedEmbargo(final MCRObjectID objectId) {
        ModifiedHandle modifiedHandle = MCRXMLMetadataManager.instance().getLastModifiedHandle(objectId, 10,
                TimeUnit.MINUTES);
        String embargo = null;
        try {
            embargo = embargoCache.getIfUpToDate(objectId, modifiedHandle);
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified timestamp of object " + objectId);
        }
        if (embargo != null) {
            return embargo == EMPTY_VALUE ? null : embargo;
        }
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(objectId));
        embargo = modsWrapper.getElementValue("mods:accessCondition[@type='embargo']");
        embargoCache.put(objectId, embargo != null ? embargo : EMPTY_VALUE);
        return embargo;
    }

    private static boolean isAfterToday(final String embargoDate) {
        try {
            final MCRISO8601Date isoED = new MCRISO8601Date(embargoDate);
            final LocalDate now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
            final LocalDate ed = LocalDate.from(isoED.getDt());

            return ed.isAfter(now);
        } catch (DateTimeException ex) {
            return embargoDate.compareTo(MCRISO8601Date.now().getISOString()) > 0;
        }
    }
}
