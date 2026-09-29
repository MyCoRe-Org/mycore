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

package org.mycore.common.xsl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.xml.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Applies a provider-specific configuration file through the JAXP {@link TransformerFactory} interface.
 * <p>
 * This class has no compile-time dependency on the provider. A relative file is resolved against the MyCoRe
 * configuration directory. If that directory is disabled, it is resolved against the current working directory.
 */
public final class MCRSaxonTransformerFactoryConfiguration implements Consumer<TransformerFactory> {

    static final String CONFIGURATION_FILE_ATTRIBUTE = "http://saxon.sf.net/feature/configuration-file";

    private static final Logger LOGGER = LogManager.getLogger();

    private String file;

    /**
     * Sets the optional provider configuration file.
     *
     * @param file absolute path or path relative to the MyCoRe configuration directory
     */
    @MCRProperty(name = "File", required = false)
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void accept(TransformerFactory factory) {
        if (file == null) {
            return;
        }
        Path configurationFile = resolveConfigurationFile();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Configuring Saxon TransformerFactory {} with {}", factory.getClass().getName(),
                configurationFile);
        }
        try {
            factory.setAttribute(CONFIGURATION_FILE_ATTRIBUTE, configurationFile.toString());
        } catch (IllegalArgumentException e) {
            throw new MCRConfigurationException("Could not apply Saxon configuration file " + configurationFile
                + " to TransformerFactory " + factory.getClass().getName(), e);
        }
    }

    private Path resolveConfigurationFile() {
        final Path configuredPath;
        try {
            configuredPath = Path.of(file);
        } catch (InvalidPathException e) {
            throw configurationException("is not a valid path", e);
        }

        Path resolvedPath = configuredPath;
        if (!configuredPath.isAbsolute()) {
            File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
            Path basePath = configurationDirectory == null ? Path.of("") : configurationDirectory.toPath();
            resolvedPath = MCRUtils.safeResolve(basePath, configuredPath);
        }
        resolvedPath = resolvedPath.toAbsolutePath().normalize();

        if (!Files.isRegularFile(resolvedPath) || !Files.isReadable(resolvedPath)) {
            throw configurationException("does not point to a readable regular file: " + resolvedPath, null);
        }
        return resolvedPath;
    }

    private MCRConfigurationException configurationException(String message, Exception cause) {
        String fullMessage = "Saxon configuration file " + message;
        return cause == null
            ? new MCRConfigurationException(fullMessage)
            : new MCRConfigurationException(fullMessage, cause);
    }

}
