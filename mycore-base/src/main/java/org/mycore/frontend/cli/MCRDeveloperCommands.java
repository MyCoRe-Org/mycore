/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRScopedSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationResolver;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.MCRResourceResolver;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;
import org.mycore.services.i18n.MCRTranslation;

import com.google.common.base.Splitter;

/**
 * This class contains commands that may be helpful during development.
 *
 * @author Torsten Krause
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@MCRCommandGroup(name = "Developer Commands")
public class MCRDeveloperCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCRDeveloperCommands.class);

    @MCRCommand(
        syntax = "show message {0} for {1}",
        help = "Show message with key {0} for locale {1}",
        order = 10)
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
        order = 20)
    public static void showMessages(String keyPrefix, String lang) {
        Map<String, String> values = MCRTranslation.translatePrefixToLocale(keyPrefix, MCRTranslation.getLocale(lang));
        if (values.isEmpty()) {
            LOGGER.info("Found no messages for key prefix {}", keyPrefix);
        } else {
            values.forEach((key, value) -> LOGGER.info("Found message for key {}: {}", key, value));
        }
    }

    @MCRCommand(
        syntax = "show all messages for {0}",
        help = "Show all messages for locale {0}",
        order = 30)
    public static void showMessages(String lang) {
        Map<String, String> values = MCRTranslation.translatePrefixToLocale("", MCRTranslation.getLocale(lang));
        if (values.isEmpty()) {
            LOGGER.info("Found no messages");
        } else {
            values.forEach((key, value) -> LOGGER.info("Found message for key {}: {}", key, value));
        }
    }

    @MCRCommand(
        syntax = "show property {0}",
        help = "Show configuration property with key {0}",
        order = 40)
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
        order = 50)
    public static void showProperties(String keyPrefix) {
        Map<String, String> values = MCRConfiguration2.getSubPropertiesMap(keyPrefix);
        if (values.isEmpty()) {
            LOGGER.info("Found no values for key prefix {}", keyPrefix);
        } else {
            values.forEach((key, value) -> LOGGER.info("Found value for key {}: {}", keyPrefix + key, value));
        }
    }

    @MCRCommand(
        syntax = "show all properties",
        help = "Show all configuration properties",
        order = 60)
    public static void showAllProperties() {
        Map<String, String> values = MCRConfiguration2.getPropertiesMap();
        if (values.isEmpty()) {
            LOGGER.info("Found no values");
        } else {
            values.forEach((key, value) -> LOGGER.info("Found value for key {}: {}", key, value));
        }
    }

    @MCRCommand(
        syntax = "check permission {0} on {1} as {2}",
        help = "Check permission {0} on {1} as {2}",
        order = 69)
    public static void checkPermission(String permission, String id, String user) throws InterruptedException {

        MCRUserInformation userInformation = MCRUserInformationResolver.instance().getOrThrow(user);

        MCRScopedSession session = (MCRScopedSession) MCRSessionMgr.getCurrentSession();
        MCRScopedSession.ScopedValues values = new MCRScopedSession.ScopedValues(userInformation);

        boolean permissionResult = session.doAs(values, () -> MCRAccessManager.checkPermission(id, permission));
        LOGGER.info(permissionResult);

    }

    @MCRCommand(
        syntax = "resolve uri {0} as {1}",
        help = "Resolve uri {0} as {1}",
        order = 70)
    public static void resolveUri(String uri, String user) throws InterruptedException {

        MCRUserInformation userInformation = MCRUserInformationResolver.instance().getOrThrow(user);

        MCRScopedSession session = (MCRScopedSession) MCRSessionMgr.getCurrentSession();
        MCRScopedSession.ScopedValues values = new MCRScopedSession.ScopedValues(userInformation);

        session.doAs(values, () -> doResolveUri(uri));

    }

    @MCRCommand(
        syntax = "resolve uri {0}",
        help = "Resolve uri {0}",
        order = 71)
    public static void resolveUri(String uri) {
        doResolveUri(uri);
    }

    private static void doResolveUri(String uri) {
        try {
            Element resource = MCRURIResolver.instance().resolve(uri);
            if (null != resource) {
                String xmlText = new XMLOutputter(Format.getPrettyFormat()).outputString(resource);
                LOGGER.info("Resolved URI {}:\n{}", uri, xmlText);
            } else {
                LOGGER.info("URI {} not found", uri);
            }
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to resolve URI " + uri, e);
        }
    }

    @MCRCommand(
        syntax = "show resource url for {0}",
        help = "Show resource URL for {0}",
        order = 80)
    public static void showResourceUri(String path) {
        showResourceUri(MCRResourcePath.ofPath(path));
    }

    @MCRCommand(
        syntax = "show web resource url for {0}",
        help = "Show web resource URL for {0}",
        order = 81)
    public static void showWebResourceUri(String path) {
        showResourceUri(MCRResourcePath.ofWebPath(path));
    }

    private static void showResourceUri(Optional<MCRResourcePath> path) {
        if (path.isEmpty()) {
            LOGGER.info("Invalid resource path");
        } else {
            doShowResourceUri(path.get());
        }
    }

    private static void doShowResourceUri(MCRResourcePath path) {
        try {
            URL url = MCRResourceResolver.instance().resolve(path).orElse(null);
            if (url != null) {
                LOGGER.info("Resolved resource {} as {}", path, url);
            } else {
                LOGGER.info("Resource {} not found", path);
            }
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to resolve resource " + path, e);
        }
    }

    @MCRCommand(
        syntax = "show all resource urls for {0}",
        help = "Show all resource URLs for {0}",
        order = 85)
    public static void showAllResourceUrls(String path) {
        showAllResourceUrls(MCRResourcePath.ofPath(path));
    }

    @MCRCommand(
        syntax = "show all web resource urls for {0}",
        help = "Show all web resource URLs for {0}",
        order = 86)
    public static void showAllWebResourceUrls(String path) {
        showAllResourceUrls(MCRResourcePath.ofWebPath(path));
    }

    private static void showAllResourceUrls(Optional<MCRResourcePath> path) {
        if (path.isEmpty()) {
            LOGGER.info("Invalid resource path");
        } else {
            doShowAllResourceUrls(path.get());
        }
    }

    private static void doShowAllResourceUrls(MCRResourcePath path) {
        try {
            List<ProvidedUrl> urls = MCRResourceResolver.instance().resolveAll(path);
            if (urls.isEmpty()) {
                LOGGER.info("Resource {} not found", path);
            } else {
                urls.forEach(url -> LOGGER.info("Resolved resource {} as {} [{}]", path, url.url, url.origin));
            }
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to resolve resource " + path, e);
        }
    }

    @MCRCommand(
        syntax = "resolve textual resource {0} with charset {1}",
        help = "Resolve textual resource {0} with charset {1}",
        order = 90)
    public static void resolveTextualResource(String path, String charset) {
        resolveTextualResource(MCRResourcePath.ofPath(path), charset);
    }

    @MCRCommand(
        syntax = "resolve textual web resource {0} with charset {1}",
        help = "Resolve textual web resource {0} with charset {1}",
        order = 91)
    public static void resolveTextualWebResource(String path, String charset) {
        resolveTextualResource(MCRResourcePath.ofWebPath(path), charset);
    }

    private static void resolveTextualResource(Optional<MCRResourcePath> path, String charset) {
        if (path.isEmpty()) {
            LOGGER.info("Invalid resource path");
        } else {
            doResolveTextualResource(path.get(), charset);
        }
    }

    private static void doResolveTextualResource(MCRResourcePath path, String charset) {
        try {
            URL url = MCRResourceResolver.instance().resolve(path).orElse(null);
            if (url != null) {
                try (InputStream is = url.openStream()) {
                    String out = new String(is.readAllBytes(), Charset.forName(charset));
                    LOGGER.info("Resolved resource {} as {}:\n{}", path, url, out);
                }
            } else {
                LOGGER.info("Resource {} not found", path);
            }
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to resolve resource " + path, e);
        }
    }

    @MCRCommand(
        syntax = "resolve textual resource {0}",
        help = "Resolve textual resource {0}",
        order = 95)
    public static void resolveTextualResource(String path) {
        resolveTextualResource(path, Charset.defaultCharset().name());
    }

    @MCRCommand(
        syntax = "resolve textual web resource {0}",
        help = "Resolve textual web resource {0}",
        order = 96)
    public static void resolveTextualWebResource(String path) {
        resolveTextualWebResource(path, Charset.defaultCharset().name());
    }

    @MCRCommand(
        syntax = "resolve binary resource {0} with encoder {1}",
        help = "Resolve binary resource {0} with encoder {1}",
        order = 100)
    public static void resolveBinaryResource(String path, String encoder) {
        resolveBinaryResource(MCRResourcePath.ofPath(path), encoder);
    }

    @MCRCommand(
        syntax = "resolve binary web resource {0} with encoder {1}",
        help = "Resolve binary web resource {0} with encoder {1}",
        order = 101)
    public static void resolveBinaryWebResource(String path, String encoder) {
        resolveBinaryResource(MCRResourcePath.ofWebPath(path), encoder);
    }

    private static void resolveBinaryResource(Optional<MCRResourcePath> path, String encoder) {
        if (path.isEmpty()) {
            LOGGER.info("Invalid resource path");
        } else {
            doResolveBinaryResource(path.get(), encoder);
        }
    }

    private static void doResolveBinaryResource(MCRResourcePath path, String encoder) {
        try {
            URL url = MCRResourceResolver.instance().resolve(path).orElse(null);
            if (url != null) {
                try (InputStream is = url.openStream();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    is.transferTo(stream);
                    String encodedContent = getEncoder(encoder).apply(stream.toByteArray());
                    LOGGER.info("Resolved resource {} as {}:\n{}", path, url, encodedContent);
                }
            } else {
                LOGGER.info("Resource {} not found", path);
            }
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to resolve resource " + path, e);
        }
    }

    private static Function<byte[], String> getEncoder(String encoder) {
        try {
            return Encoder.valueOf(encoder);
        } catch (IllegalArgumentException e) {
            String encoders = Arrays.stream(Encoder.values()).map(Encoder::toString).collect(Collectors.joining(", "));
            throw new MCRException("Encoder '" + encoder + "' unknown; must be one of: " + encoders, e);
        }
    }

    @MCRCommand(
        syntax = "resolve binary resource {0}",
        help = "Resolve binary resource {0}",
        order = 105)
    public static void resolveBinaryResource(String path) {
        resolveBinaryResource(path, Encoder.BASE_64.name());
    }

    @MCRCommand(
        syntax = "resolve binary web resource {0}",
        help = "Resolve binary web resource {0}",
        order = 106)
    public static void resolveBinaryWebResource(String path) {
        resolveBinaryWebResource(path, Encoder.BASE_64.name());
    }

    @MCRCommand(
        syntax = "touch object {0}",
        help = "Load and update object with id {0} without making any modifications",
        order = 110)
    public static void touchObject(String id) {
        try {
            MCRObjectID objectId = MCRObjectID.getInstance(id);
            MCRObject object = MCRMetadataManager.retrieveMCRObject(objectId);
            MCRMetadataManager.update(object);
            LOGGER.info("Touched object with id {}", id);
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to touch object with id " + id, e);
        }
    }

    @MCRCommand(
        syntax = "touch derivate {0}",
        help = "Load and update derivate with id {0} without making any modifications",
        order = 111)
    public static void touchDerivate(String id) {
        try {
            MCRObjectID derivateId = MCRObjectID.getInstance(id);
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            MCRMetadataManager.update(derivate);
            LOGGER.info("Touched derivate with id {}", id);
        } catch (Exception e) {
            LOGGER.info(() -> "Failed to touch derivate with id " + id, e);
        }
    }

    private enum Encoder implements Function<byte[], String> {

        BASE_64 {
            @Override
            public String apply(byte[] bytes) {
                String base64 = Base64.getEncoder().encodeToString(bytes);
                StringBuilder builder = new StringBuilder();
                Splitter.fixedLength(80).split(base64).forEach(line -> builder.append(line).append('\n'));
                return builder.toString();
            }

        };

    }

}
