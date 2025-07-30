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

package org.mycore.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

/**
 * JUnit 5 extension for setting up and tearing down the MCR metadata store and SVN directories.
 * This extension creates temporary directories for the metadata store and SVN base directory,
 * and cleans them up after each test.
 * <p>
 * Sets the following properties in the class properties:
 * <dl>
 * <dt><code>MCR.Metadata.Store.BaseDir</code></dt><dd>Path to the metadata store base directory</dd>
 * <dt><code>MCR.Metadata.Store.SVNBase</code></dt><dd>URI of the SVN base directory</dd>
 * </dl>
 *
 */
public class MCRMetadataExtension implements Extension, BeforeAllCallback, BeforeEachCallback,
    AfterEachCallback {

    private static final String STORE_BASE_DIR_KEY = "MCRMetadataExtension.storeBaseDir";
    private static final String SVN_BASE_DIR_KEY = "MCRMetadataExtension.svnBaseDir";
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Path baseDirPath = MCRTestExtensionConfigurationHelper.getBaseDir();
        if (!baseDirPath.toFile().isDirectory()) {
            Files.createDirectories(baseDirPath);
        }
        // Create temp directories programmatically
        Path storeBaseDir = Files.createTempDirectory(baseDirPath, "mcr-store-");
        Path svnBaseDir = Files.createTempDirectory(baseDirPath, "mcr-svn-");

        // Store paths in the extension context store
        context.getStore(MCRTestExtension.NAMESPACE).put(STORE_BASE_DIR_KEY, storeBaseDir);
        context.getStore(MCRTestExtension.NAMESPACE).put(SVN_BASE_DIR_KEY, svnBaseDir);

        // Set up properties in class properties
        Map<String, String> classProperties = MCRTestExtension.getClassProperties(context);
        classProperties.put("MCR.Metadata.Store.BaseDir", storeBaseDir.toAbsolutePath().toString());
        classProperties.put("MCR.Metadata.Store.SVNBase", svnBaseDir.toUri().toString());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        MCRConfiguration2.getSubPropertiesMap("MCR.Metadata.Store.").forEach(
            (key, value) -> LOGGER.debug("MCR Metadata Store Property: {}={}", key, value));
        Files.createDirectories(getStoreBaseDir(context));
        Files.createDirectories(getSvnBaseDir(context));
        MCRXMLMetadataManager.getInstance().reload();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Clean up temp directories
        Path storeBaseDir = getStoreBaseDir(context);
        MCRTestHelper.deleteRecursively(storeBaseDir);

        Path svnBaseDir = getSvnBaseDir(context);
        SVNFileUtil.deleteAll(svnBaseDir.toFile(), true);
    }

    /**
     * Returns the store base directory.
     * @param context the current extension context
     * @return Path to store base directory
     */
    public static Path getStoreBaseDir(ExtensionContext context) {
        return context.getStore(MCRTestExtension.NAMESPACE).get(STORE_BASE_DIR_KEY, Path.class);
    }

    /**
     * Returns the SVN base directory.
     * @param context the current extension context
     * @return Path to SVN base directory
     */
    public static Path getSvnBaseDir(ExtensionContext context) {
        return context.getStore(MCRTestExtension.NAMESPACE).get(SVN_BASE_DIR_KEY, Path.class);
    }

}
