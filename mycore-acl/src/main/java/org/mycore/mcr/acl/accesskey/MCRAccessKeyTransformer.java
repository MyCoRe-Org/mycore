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

package org.mycore.mcr.acl.accesskey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyTransformationException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Methods to transform betwenn {@link MCRAccessKey} and JSON.
 */
public class MCRAccessKeyTransformer {

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
     * @return the MCRAccessKey
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static MCRAccessKey accessKeyFromJson(String json) {
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
     * @return the MCRAccessKey list
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static List<MCRAccessKey> accessKeysFromJson(String json) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Arrays.asList(objectMapper.readValue(json, MCRAccessKey[].class));
        } catch (JsonProcessingException e) {
            throw new MCRAccessKeyTransformationException("Invalid JSON.");
        }
    }

    /**
     * Transforms a {@link MCRAccessKey} to JSON.
     * 
     * @param accessKey the MCRAccessKey
     * @return access key as json string
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static String jsonFromAccessKey(MCRAccessKey accessKey) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(accessKey);
        } catch (JsonProcessingException e) {
            throw new MCRAccessKeyTransformationException("Access key could not be converted to JSON.");
        }
    }

    /**
     * Transforms a {@link MCRAccessKey} list to JSON.
     * 
     * @param accessKeys the MCRAccessKey list
     * @return access keys as json array string
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static String jsonFromAccessKeys(List<MCRAccessKey> accessKeys) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(accessKeys);
        } catch (JsonProcessingException e) {
            throw new MCRAccessKeyTransformationException("Access keys could not be converted to JSON.");
        }
    }

    /**
     * Transforms service element to {@link MCRAccessKey} list.
     * 
     * @param objectId the linked MCRObjectID
     * @param element the service element
     * @return the MCRAccessKey list
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static List<MCRAccessKey> accessKeysFromElement(MCRObjectID objectId, Element element) {
        if (Objects.equals(element.getName(), ROOT_SERVICE)) {
            Element servFlagsRoot = element.getChild(ROOT_SERV_FLAGS);
            if (servFlagsRoot != null) {
                final List<Element> servFlags = servFlagsRoot.getChildren(SERV_FLAG);
                for (Element servFlag : servFlags) {
                    if (Objects.equals(servFlag.getAttributeValue("type"), ACCESS_KEY_TYPE)) {
                        return accessKeysFromAccessKeyElement(objectId, servFlag);
                    }
                }
            }
        } else if (Objects.equals(element.getName(), SERV_FLAG)
            && Objects.equals(element.getAttributeValue("type"), ACCESS_KEY_TYPE)) {
            return accessKeysFromAccessKeyElement(objectId, element);
        } else if (Objects.equals(element.getName(), ACCESS_KEY_TYPE)) {
            return accessKeysFromAccessKeyElement(objectId, element);
        }
        return new ArrayList<>();
    }

    /**
     * Transforms {@link MCRAccessKey} list to a element.
     * 
     * @param accessKeys the MCRAccessKey list
     * @return the accesskeys element with access key list as json string as content
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    public static Element elementFromAccessKeys(List<MCRAccessKey> accessKeys) {
        final String jsonString = jsonFromAccessKeys(accessKeys);
        final Element element = elementFromAccessKeysJson(jsonString);
        element.setAttribute("count", Integer.toString(accessKeys.size()));
        return element;
    }

    /**
     * Transforms servflag element to {@link MCRAccessKey} list.
     * 
     * @param objectId the linked MCRObjectID
     * @param element servlag element {@link org.mycore.datamodel.metadata.MCRObjectService}
     * @return the MCRAccessKey list
     * @throws MCRAccessKeyTransformationException if the transformation fails
     */
    private static List<MCRAccessKey> accessKeysFromAccessKeyElement(MCRObjectID objectId, Element element) {
        return accessKeysFromJson(element.getText()).stream().peek(a -> a.setObjectId(objectId)).toList();
    }

    /**
     * Transforms JSON of {@link MCRAccessKey} list to a element.
     * 
     * @param json the JSON
     * @return element with accesskeys name
     */
    private static Element elementFromAccessKeysJson(String json) {
        final Element element = new Element(ACCESS_KEY_TYPE);
        element.setText(json);
        return element;
    }
}
