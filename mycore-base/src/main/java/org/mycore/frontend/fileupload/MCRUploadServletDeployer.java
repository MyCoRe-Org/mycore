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

package org.mycore.frontend.fileupload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration.Dynamic;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * Uses <code>mycore.properties</code> to configure {@link MCRUploadViaFormServlet}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUploadServletDeployer implements AutoExecutable {

    private static final String MCR_FILE_UPLOAD_TEMP_STORAGE_PATH = "MCR.FileUpload.TempStoragePath";

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return MCRUploadViaFormServlet.class.getSimpleName() + " Deployer";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(javax.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            String servletName = "MCRUploadViaFormServlet";
            MultipartConfigElement multipartConfig = getMultipartConfig();
            try {
                checkTempStoragePath(multipartConfig.getLocation());
            } catch (IOException e) {
                throw new MCRConfigurationException("Could not setup " + servletName + "!", e);
            }
            Dynamic uploadServlet = servletContext.addServlet(servletName, MCRUploadViaFormServlet.class);
            uploadServlet.addMapping("/servlets/MCRUploadViaFormServlet");
            uploadServlet.setMultipartConfig(multipartConfig);
        }
    }

    private void checkTempStoragePath(String location) throws IOException {
        Path targetDir = Paths.get(location);
        if (!targetDir.isAbsolute()) {
            throw new MCRConfigurationException(
                "'" + MCR_FILE_UPLOAD_TEMP_STORAGE_PATH + "=" + location + "' must be an absolute path!");
        }
        if (Files.notExists(targetDir)) {
            LogManager.getLogger().info("Creating directory: {}", targetDir);
            Files.createDirectories(targetDir);
        }
        if (!Files.isDirectory(targetDir)) {
            throw new NotDirectoryException(targetDir.toString());
        }
    }

    private MultipartConfigElement getMultipartConfig() {
        String location = MCRConfiguration2.getStringOrThrow(MCR_FILE_UPLOAD_TEMP_STORAGE_PATH);
        long maxFileSize = MCRConfiguration2.getLong("MCR.FileUpload.MaxSize").orElse(5000000L);
        int fileSizeThreshold = MCRConfiguration2.getInt("MCR.FileUpload.MemoryThreshold").orElse(1000000);
        LogManager.getLogger()
            .info(() -> MCRUploadViaFormServlet.class.getSimpleName() + " accept files and requests up to "
                + maxFileSize + " bytes and uses " + location + " as tempory storage for files larger "
                + fileSizeThreshold + " bytes.");
        return new MultipartConfigElement(location, maxFileSize, maxFileSize,
            fileSizeThreshold);
    }

}
