package org.mycore.services.staticcontent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private Path getSlotDirPath(MCRObjectID id) {
        final String numberAsString = id.getNumberAsString();

        Path result = getStaticFileRootPath().resolve(numberAsString.substring(0, 3))
            .resolve(numberAsString.substring(3, 6));
        LOGGER.debug(() -> "Resolved slot path is" + result.toString());
        return result;
    }

    public Path getStaticFileRootPath() {
        return this.staticFileRootPath;
    }

    public MCRContentTransformer getTransformer() {
        return transformer;
    }
}
