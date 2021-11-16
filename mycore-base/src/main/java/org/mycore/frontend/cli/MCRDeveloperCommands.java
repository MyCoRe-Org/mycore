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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.services.i18n.MCRTranslation;

import java.util.Map;

/**
 * This class contains commands that may be helpful during development.
 *
 * @author Torsten Krause
 */

@MCRCommandGroup(name = "Developer Commands")
public class MCRDeveloperCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCRDeveloperCommands.class);

    @MCRCommand(
            syntax = "show message {0} for {1}",
            help = "Show message with key {0} for locale {1}",
            order = 10
    )
    public static void showMessage(String key, String lang) {
        String value = MCRTranslation.translate(key, MCRTranslation.getLocale(lang));
        if (value == null || (value.startsWith("???") && value.endsWith("???"))) {
            LOGGER.info("Found no message for key {}", key);
        } else {
            LOGGER.info("Found message for key {}: {}", key, value);
        }
    }

    @MCRCommand(
            syntax = "show messages {0} for {1}",
            help = "Show messages with key prefix {0} for locale {1}",
            order = 20
    )
    public static void showMessages(String keyPrefix, String lang) {
        Map<String, String> values = MCRTranslation.translatePrefix(keyPrefix, MCRTranslation.getLocale(lang));
        if (values.isEmpty()) {
            LOGGER.info("Found no messages for key prefix {}", keyPrefix);
        } else {
            values.forEach((key, value) -> {
                LOGGER.info("Found message for key {}: {}", key, value);
            });
        }
    }

    @MCRCommand(
            syntax = "show all messages for {0}",
            help = "Show all messages for locale {0}",
            order = 30
    )
    public static void showMessages(String lang) {
        Map<String, String> values = MCRTranslation.translatePrefix("", MCRTranslation.getLocale(lang));
        if (values.isEmpty()) {
            LOGGER.info("Found no messages");
        } else {
            values.forEach((key, value) -> {
                LOGGER.info("Found message for key {}: {}", key, value);
            });
        }
    }

    @MCRCommand(
            syntax = "show property {0}",
            help = "Show configuration property with key {0}",
            order = 40
    )
    public static void showProperty(String key) {
        String value = MCRConfiguration2.getPropertiesMap().get(key);
        if (value == null) {
            LOGGER.info("Found no value for key {}", key);
        } else {
            LOGGER.info("Found value for key {}: {}", key, value);
        }
    }

    @MCRCommand(
            syntax = "show properties {0}",
            help = "Show configuration properties starting with key prefix {0}",
            order = 50
    )
    public static void showProperties(String keyPrefix) {
        Map<String, String> values = MCRConfiguration2.getSubPropertiesMap(keyPrefix);
        if (values.isEmpty()) {
            LOGGER.info("Found no values for key prefix {}", keyPrefix);
        } else {
            values.forEach((key, value) -> {
                LOGGER.info("Found value for key {}: {}", keyPrefix + key, value);
            });
        }
    }

    @MCRCommand(
            syntax = "show all properties",
            help = "Show all configuration properties",
            order = 60
    )
    public static void showAllProperties() {
        Map<String, String> values = MCRConfiguration2.getPropertiesMap();
        if (values.isEmpty()) {
            LOGGER.info("Found no values");
        } else {
            values.forEach((key, value) -> {
                LOGGER.info("Found value for key {}: {}", key, value);
            });
        }
    }

    @MCRCommand(
            syntax = "show resource {0}",
            help = "Show resource with uri {0}",
            order = 70
    )
    public static void showResource(String uri) {
        try {
            Element resource = MCRURIResolver.instance().resolve(uri);
            String xmlText = new XMLOutputter(Format.getPrettyFormat()).outputString(resource);
            LOGGER.info("Resolved resource for uri {}:\n{}", uri, xmlText);
        } catch (Exception e) {
            LOGGER.info("Failed to resolve resource for uri " + uri, e);
        }
    }


    @MCRCommand(
            syntax = "touch object {0}",
            help = "Load and update object with id {0} without making any modifications",
            order = 80
    )
    public static void touchObject(String id) {
        try {
            MCRObjectID objectId = MCRObjectID.getInstance(id);
            MCRObject object = MCRMetadataManager.retrieveMCRObject(objectId);
            MCRMetadataManager.update(object);
            LOGGER.info("Touched object with id {}", id);
        } catch (Exception e) {
            LOGGER.info("Failed to touch object with id " + id, e);
        }
    }

    @MCRCommand(
            syntax = "touch derivate {0}",
            help = "Load and update derivate with id {0} without making any modifications",
            order = 90
    )
    public static void touchDerivate(String id) {
        try {
            MCRObjectID derivateId = MCRObjectID.getInstance(id);
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            MCRMetadataManager.update(derivate);
            LOGGER.info("Touched derivate with id {}", id);
        } catch (Exception e) {
            LOGGER.info("Failed to touch derivate with id " + id, e);
        }
    }

}
