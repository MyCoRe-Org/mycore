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

package org.mycore.orcid2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.flag.MCRORCIDFlagContent;
import org.mycore.orcid2.flag.MCRORCIDUserInfo;

/**
 * Handles metadata for ORCID stuff.
 */
public class MCRORCIDMetadataService {

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
        final String flagContentString = object.getService().getFlags(ORCID_FLAG).stream().findFirst().orElse(null);
        if (flagContentString == null) {
            return null;
        } 
        try {
            return transformFlagContentString(flagContentString);
        } catch (MCRORCIDTransformationException e) {
            throw new MCRORCIDException("ORCID flag of object " + object.getId() + "is broken" , e);
        }
    }

    /**
     * Sets ORCID flag of MCRObject.
     * 
     * @param object the MCRObject
     * @param flagContent the MCRORCIDFlagContent
     * @throws MCRORCIDException if update fails
     */
    public static void setORCIDFlagContent(MCRObject object, MCRORCIDFlagContent flagContent) throws MCRORCIDException {
        try {
            object.getService().removeFlags(ORCID_FLAG);
            final String flagContentString = transformFlagContent(flagContent);
            object.getService().addFlag(ORCID_FLAG, flagContentString);
            MCRMetadataManager.update(object);
        } catch (MCRAccessException | MCRPersistenceException | MCRORCIDTransformationException e) {
            throw new MCRORCIDException("Could not update list of object " + object.getId(), e);
        }
    }

    /**
     * Removes ORCID flag from MCRObject.
     * 
     * @param object the MCRObject
     * @throws MCRORCIDException if cannot remove flag
     */
    public static void removeORCIDFlag(MCRObject object) throws MCRORCIDException {
        object.getService().removeFlags(ORCID_FLAG);
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
        final MCRORCIDFlagContent flagContent = getORCIDFlagContent(object);
        if (flagContent == null) {
            return null;
        }
        return flagContent.getUserInfoByORCID(orcid);
    }

    /**
     * Updates MCRORCIDUserInfo by ORCID iD for MCROject.
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @param userInfo the MCRORCIDUserInfo
     * @throws MCRORCIDException if update fails
     */
    public static void updateUserInfoByORCID(MCRObject object, String orcid, MCRORCIDUserInfo userInfo)
        throws MCRORCIDException {
        final MCRORCIDFlagContent flagContent = getORCIDFlagContent(object);
        if (flagContent == null) {
            throw new MCRORCIDException("Flag does not exist");
        }
        flagContent.updateUserInfoByORCID(orcid, userInfo);
        setORCIDFlagContent(object, flagContent);
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
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(flagContent);
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
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(flagContentString, MCRORCIDFlagContent.class);
        } catch (JsonProcessingException e) {
            throw new MCRORCIDTransformationException(e);
        }
    }
}
