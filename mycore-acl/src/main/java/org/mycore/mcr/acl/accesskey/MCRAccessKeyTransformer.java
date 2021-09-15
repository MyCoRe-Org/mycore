/*
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

package org.mycore.mcr.acl.accesskey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyTransformationException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Methods for transforming {@link MCRAccessKey} between JSON.
 */
public class MCRAccessKeyTransformer {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Name of service element.
     */
    private static final String ROOT_SERVICE = "service";

    /**
     * Name of servflags element.
     */
    private static final String ROOT_SERV_FLAGS = "servflags";
    
    /**
     * Name of servflag element.
     */
    private static final String SERV_FLAG = "servflag";

    /**
     * Name of accesskeys element.
     */
    public static final String ACCESS_KEY_TYPE = "accesskeys";

    /**
     * Transforms JSON to a {@link MCRAccessKey}.
     *
     * @param json the json
     * @return the {@link MCRAccessKey}
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static MCRAccessKey accessKeyFromJson(final String json) 
        throws MCRAccessKeyTransformationException {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, MCRAccessKey.class);
        } catch (JsonProcessingException e) {
            throw new MCRAccessKeyTransformationException("Cannot transform JSON.");
        }
    }

    /**
     * Transforms JSON to {@link MCRAccessKey} list.
     *
     * @param json the json
     * @return the {@link MCRAccessKey}
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static List<MCRAccessKey> accessKeysFromJson(final String json) 
        throws MCRAccessKeyTransformationException {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Arrays.asList(objectMapper.readValue(json, MCRAccessKey[].class));
        } catch (JsonProcessingException e) {
            throw new MCRAccessKeyTransformationException("Invalid JSON.");
        }
    }

    /**
     * Transforms a {@link MCRAccessKey} list to JSON.
     *
     * @param accessKeys the {@link MCRAccessKey} list
     * @return JSON or null if the transformation fails
     */
    public static String jsonFromAccessKeys(final List<MCRAccessKey> accessKeys) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(accessKeys);
        } catch (JsonProcessingException e) { //should not happen
            LOGGER.warn("Access keys could not be converted to JSON.");
            return null;
        }
    }

    /**
     * Transforms service element to {@link MCRAccessKey} list
     *
     * @param objectId the linked {@link MCRObjectID}
     * @param element the service element
     * @return the {@link MCRAccessKey} list
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static List<MCRAccessKey> accessKeysFromElement(MCRObjectID objectId, Element element)
        throws MCRAccessKeyTransformationException {
        if (element.getName().equals(ROOT_SERVICE)) {
            Element servFlagsRoot = element.getChild(ROOT_SERV_FLAGS);
            if (servFlagsRoot != null) {
                final List<Element> servFlags = servFlagsRoot.getChildren(SERV_FLAG);
                for (Element servFlag : servFlags) {
                    if (servFlag.getAttributeValue("type").equals(ACCESS_KEY_TYPE)) {
                        return accessKeysFromServFlag(objectId, servFlag);
                    }
                }
            }
        } else if (element.getName().equals(SERV_FLAG) && ACCESS_KEY_TYPE.equals(element.getAttributeValue("type"))) {
            return accessKeysFromServFlag(objectId, element);
        } 
        return new ArrayList<>();
    }

    /**
     * Transforms servflag element to {@link MCRAccessKey} list
     *
     * @param objectId the linked {@link MCRObjectID}
     * @param servFlag servlag element {@link org.mycore.datamodel.metadata.MCRObjectService}
     * @return the {@link MCRAccessKey} list
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    private static List<MCRAccessKey> accessKeysFromServFlag(MCRObjectID objectId, Element servFlag)
        throws MCRAccessKeyTransformationException {
        final String json = servFlag.getText();
        final List<MCRAccessKey> accessKeyList = accessKeysFromJson(json);
        for (MCRAccessKey accessKey : accessKeyList) {
            accessKey.setObjectId(objectId);
        }
        return accessKeyList;
    }

    /**
     * Transforms {@link MCRAccessKey} list to a servflag
     *
     * @param accessKeys the {@link MCRAccessKey} list
     * @return the servlag or null if there is no {@link MCRAccessKey}
     */
    public static Element servFlagFromAccessKeys(final List<MCRAccessKey> accessKeys) {
        final String jsonString = jsonFromAccessKeys(accessKeys);
        if (jsonString != null) {
            return servFlagfromAccessKeysJson(jsonString);
        }
        return new Element("null");
    }

    /**
     * Transforms JSON of {@link MCRAccessKey} list to a servflag element
     *
     * @param json the JSON
     * @return the servlag
     */
    private static Element servFlagfromAccessKeysJson(final String json) {
        final Element servFlag = new Element(SERV_FLAG);
        servFlag.setAttribute("type", ACCESS_KEY_TYPE);
        servFlag.setAttribute("inherited", "0");
        servFlag.setAttribute("form", "plain");
        servFlag.setText(json);
        return servFlag;
    } 
}
