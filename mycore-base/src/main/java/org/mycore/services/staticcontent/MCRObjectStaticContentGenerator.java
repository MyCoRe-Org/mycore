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

package org.mycore.services.staticcontent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectStaticContentGenerator {

    public static final String TRANSFORMER_SUFFIX = ".Transformer";

    public static final String ROOT_PATH_SUFFIX = ".Path";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_ID_PREFIX = "MCR.Object.Static.Content.Generator.";

    private static final String DEFAULT_TRANSFORMER_PATH_PROPERTY = "MCR.Object.Static.Content.Default.Path";

    private static final String CLASS_SUFFIX = ".Class";

    private MCRContentTransformer transformer;

    private Path staticFileRootPath;

    public MCRObjectStaticContentGenerator(String configID) {
        this(
            MCRConfiguration2.getString(CONFIG_ID_PREFIX + configID + TRANSFORMER_SUFFIX).orElseThrow(
                () -> new MCRConfigurationException(
                    "The suffix " + TRANSFORMER_SUFFIX + " is not set for " + CONFIG_ID_PREFIX + configID)),
            MCRConfiguration2.getString(CONFIG_ID_PREFIX + configID + ROOT_PATH_SUFFIX)
                .orElseGet(() -> MCRConfiguration2.getStringOrThrow(DEFAULT_TRANSFORMER_PATH_PROPERTY) + "/"
                    + configID));
    }

    protected MCRObjectStaticContentGenerator(String transformer, String staticFileRootPath) {
        this(MCRContentTransformerFactory.getTransformer(transformer),
            staticFileRootPath != null ? Paths.get(staticFileRootPath) : null);
    }

    protected MCRObjectStaticContentGenerator(MCRContentTransformer transformer, Path staticFileRootPath) {
        this.transformer = transformer;
        this.staticFileRootPath = staticFileRootPath;
    }

    static MCRObjectStaticContentGenerator get(String id) {
        try {
            return MCRConfiguration2.getString(CONFIG_ID_PREFIX + id + CLASS_SUFFIX)
                .map(c -> {
                    try {
                        return (Class<MCRObjectStaticContentGenerator>) MCRClassTools.forName(c);
                    } catch (ClassNotFoundException e) {
                        throw new MCRException(e);
                    }
                }).orElse(MCRObjectStaticContentGenerator.class).getDeclaredConstructor(String.class).newInstance(id);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
            | NoSuchMethodException e) {
            throw new MCRException(e);
        }
    }

    public static List<String> getContentGenerators() {
        return MCRConfiguration2.getPropertiesMap()
            .keySet()
            .stream()
            .filter(k -> k.startsWith(CONFIG_ID_PREFIX))
            .map(wholeProperty -> {
                String propertySuffix = wholeProperty.substring(CONFIG_ID_PREFIX.length());
                final int i = propertySuffix.indexOf('.');
                if (i > 0) {
                    return propertySuffix.substring(0, i);
                }
                return propertySuffix;
            })
            .distinct()
            .collect(Collectors.toList());
    }

    public void generate(MCRObject object) throws IOException {
        LOGGER.debug(() -> "Create static content with " + getTransformer() + " for " + object);
        final MCRObjectID objectID = object.getId();

        if (!filter(object)) {
            return;
        }

        final Path slotDirPath = getSlotDirPath(objectID);
        if (!Files.exists(slotDirPath)) {
            Files.createDirectories(slotDirPath);
        }

        final MCRContent result = getTransformer().transform(new MCRBaseContent(object));

        final Path filePath = getFilePath(objectID);
        try (OutputStream os = Files
            .newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            result.sendTo(os);
        }
    }

    public InputStream get(MCRObjectID id) throws IOException {
        return Files.newInputStream(getFilePath(id));
    }

    private Path getFilePath(MCRObjectID id) throws IOException {
        return getSlotDirPath(id).resolve(id.toString().concat(".xml"));
    }

    protected Path getSlotDirPath(MCRObjectID id) {
        final String numberAsString = id.getNumberAsString();

        final int folderSize = 3;
        final int folders = (int) Math.ceil(numberAsString.length() / (double) folderSize);
        Path result = getStaticFileRootPath();
        for (int i = 0; i < folders; i++) {
            result = result.resolve(
                numberAsString.substring(folderSize * i, Math.min(folderSize * i + 3, numberAsString.length())));
        }
        final Path finalResult = result;
        LOGGER.debug(() -> "Resolved slot path is" + finalResult.toString());
        return result;
    }

    /**
     * Allows to implement an own instance which filters if the object is suitable to create static content.
     * The class can be defined by appending .Class after the id just like with transformer
     * E.g. we do not want to run the epicur stylesheets with no urn
     * @param object the object to check
     * @return true if the object is suitable
     */
    protected boolean filter(MCRObject object) {
        return true;
    }

    public Path getStaticFileRootPath() {
        return this.staticFileRootPath;
    }

    public MCRContentTransformer getTransformer() {
        return transformer;
    }
}
