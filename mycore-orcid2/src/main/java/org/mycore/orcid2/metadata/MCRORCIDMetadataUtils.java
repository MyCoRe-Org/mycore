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

package org.mycore.orcid2.metadata;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;

/**
 * Handles metadata for ORCID stuff.
 */
public class MCRORCIDMetadataUtils {

    /**
     * Controls the saving of other put codes.
     */
    public static boolean SAVE_OTHER_PUT_CODES = MCRConfiguration2.getOrThrow(MCRORCIDConstants.CONFIG_PREFIX
        + "Metadata.WorkInfo.SaveOtherPutCodes", Boolean::parseBoolean);

    /**
     * Name of ORCID flag.
     */
    protected static final String ORCID_FLAG = "MyCoRe-ORCID";

    /**
     * Returns MCRORCIDFlagContent of orcid flag.
     * 
     * @param object the MCRObject
     * @return the MCRORCIDFlagContent or null
     * @throws MCRORCIDException if orcid flag is broken
     */
    public static MCRORCIDFlagContent getORCIDFlagContent(MCRObject object) throws MCRORCIDException {
        try {
            return getORCIDFlagContentString(object).map(s -> transformFlagContentString(s)).orElse(null);
        } catch (MCRORCIDTransformationException e) {
            throw new MCRORCIDException("ORCID flag of object " + object.getId() + "is broken", e);
        }
    }

    /**
     * Sets ORCID flag of MCRObject and updates MCRObject.
     * 
     * @param object the MCRObject
     * @param flagContent the MCRORCIDFlagContent
     * @throws MCRORCIDException if update fails
     */
    public static void setORCIDFlagContent(MCRObject object, MCRORCIDFlagContent flagContent) throws MCRORCIDException {
        try {
            doSetORCIDFlagContent(object, flagContent);
            MCRMetadataManager.update(object);
        } catch (MCRAccessException | MCRPersistenceException e) {
            throw new MCRORCIDException("Could not update list of object " + object.getId(), e);
        }
    }

    /**
     * Removes ORCID flag from MCRObject and updates MCRObject.
     * 
     * @param object the MCRObject
     * @throws MCRORCIDException if cannot remove flag
     */
    public static void removeORCIDFlag(MCRObject object) throws MCRORCIDException {
        doRemoveORCIDFlag(object);
        try {
            MCRMetadataManager.update(object);
        } catch (Exception e) {
            throw new MCRORCIDException("Could remove list of object " + object.getId(), e);
        }
    }

    /**
     * Returns MCRORCIDUserInfo by ORCID iD for MCRObject.
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @return the MCRORCIDUserInfo or null
     * @throws MCRORCIDException if MCRORCIDUserInfo is broken
     */
    public static MCRORCIDUserInfo getUserInfoByORCID(MCRObject object, String orcid) throws MCRORCIDException {
        return Optional.ofNullable(getORCIDFlagContent(object)).map(f -> f.getUserInfoByORCID(orcid)).orElse(null);
    }

    /**
     * Updates MCRORCIDUserInfo by ORCID iD for MCROject and updates MCRObject.
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @param userInfo the MCRORCIDUserInfo
     * @throws MCRORCIDException if update fails
     */
    public static void updateUserInfoByORCID(MCRObject object, String orcid, MCRORCIDUserInfo userInfo)
        throws MCRORCIDException {
        final MCRORCIDFlagContent flagContent = Optional.ofNullable(getORCIDFlagContent(object))
            .orElseThrow(() -> new MCRORCIDException("Flag does not exist"));
        flagContent.updateUserInfoByORCID(orcid, userInfo);
        setORCIDFlagContent(object, flagContent);
    }

    /**
     * Removes all work infos excluding orcids
     * 
     * @param object the MCRObject
     * @param orcids List of ORCID iDs
     * @throws MCRORCIDException if clean up fails
     */
    public static void cleanUpWorkInfosExcludingORCIDs(MCRObject object, List<String> orcids) throws MCRORCIDException {
        final MCRORCIDFlagContent flagContent = getORCIDFlagContent(object);
        // nothing to clean up
        if (flagContent == null) {
            return;
        }
        flagContent.getUserInfos().forEach(i -> {
            if (!orcids.contains(i.getORCID())) {
                i.setWorkInfo(null);
            }
        });
        setORCIDFlagContent(object, flagContent);
    }

    /**
     * Removes ORCID flag from MCRObject.
     * 
     * @param object the MCRObject
     */
    protected static void doRemoveORCIDFlag(MCRObject object) throws MCRORCIDException {
        object.getService().removeFlags(ORCID_FLAG);
    }

    /**
     * Sets ORCID flag of MCRObject.
     * 
     * @param object the MCRObject
     * @param flagContent the MCRORCIDFlagContent
     * @throws MCRORCIDException if update fails
     */
    protected static void doSetORCIDFlagContent(MCRObject object, MCRORCIDFlagContent flagContent)
        throws MCRORCIDException {
        removeORCIDFlags(object);
        if (!SAVE_OTHER_PUT_CODES) {
            // may rudimentary approach
            flagContent.getUserInfos().stream().map(MCRORCIDUserInfo::getWorkInfo).filter(Objects::nonNull)
                .forEach(w -> w.setOtherPutCodes(null));
        }
        String flagContentString = null;
        try {
            flagContentString = transformFlagContent(flagContent);
        } catch (MCRORCIDTransformationException e) {
            throw new MCRORCIDException("Could not update list of object " + object.getId(), e);
        }
        addORCIDFlag(object, flagContentString);
    }

    /**
     * Transforms MCRORCIDFlagContent to String.
     * 
     * @param flagContent the MCRORCIDFlagContent
     * @return MCRORCIDFlagContent as String
     * @throws MCRORCIDTransformationException if collection transformation fails
     */
    protected static String transformFlagContent(MCRORCIDFlagContent flagContent)
        throws MCRORCIDTransformationException {
        try {
            return new ObjectMapper().writeValueAsString(flagContent);
        } catch (JsonProcessingException e) {
            throw new MCRORCIDTransformationException(e);
        }
    }

    /**
     * Transforms flag content as String to MCRORCIDFlagContent.
     * 
     * @param flagContentString MCRORCIDFlagContent as String
     * @return MCRORCIDFlagContent
     * @throws MCRORCIDTransformationException if string transformation fails
     */
    protected static MCRORCIDFlagContent transformFlagContentString(String flagContentString)
        throws MCRORCIDTransformationException {
        try {
            return new ObjectMapper().readValue(flagContentString, MCRORCIDFlagContent.class);
        } catch (JsonProcessingException e) {
            throw new MCRORCIDTransformationException(e);
        }
    }

    private static Optional<String> getORCIDFlagContentString(MCRObject object) {
        return object.getService().getFlags(ORCID_FLAG).stream().findFirst();
    }

    private static void addORCIDFlag(MCRObject object, String content) {
        object.getService().addFlag(ORCID_FLAG, content);
    }

    private static void removeORCIDFlags(MCRObject object) {
        object.getService().removeFlags(ORCID_FLAG);
    }
}
