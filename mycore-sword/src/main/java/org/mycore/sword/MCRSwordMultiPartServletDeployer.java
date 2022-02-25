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

package org.mycore.sword;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;
import org.mycore.sword.servlets.MCRSwordCollectionServlet;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.http.HttpServlet;

/**
 * Uses <code>mycore.properties</code> to configure {@link org.mycore.sword.servlets.MCRSwordCollectionServlet},
 * {@link org.mycore.sword.servlets.MCRSwordContainerServlet} and
 * {@link org.mycore.sword.servlets.MCRSwordMediaResourceServlet}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSwordMultiPartServletDeployer implements AutoExecutable {

    private static final String UPLOAD_TEMP_STORAGE_PATH = "MCR.SWORD.FileUpload.TempStoragePath";

    private static final List<ServletConfig> MULTIPART_SERVLETS = List.of(
        new ServletConfig("MCRSword2CollectionServlet", "/sword2/col/*", MCRSwordCollectionServlet.class),
        new ServletConfig("MCRSword2ContainerServlet", "/sword2/edit/*", MCRSwordCollectionServlet.class),
        new ServletConfig("MCRSword2MediaResourceServlet", "/sword2/edit-media/*", MCRSwordCollectionServlet.class));

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return "mycore-sword Servlet Deployer";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(jakarta.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            MultipartConfigElement multipartConfig = getMultipartConfig();
            try {
                checkTempStoragePath(multipartConfig.getLocation());
            } catch (IOException e) {
                throw new MCRConfigurationException("Could not setup mycore-sword servlets!", e);
            }
            MULTIPART_SERVLETS.forEach(config -> {
                LogManager.getLogger()
                    .info(() -> "Mapping " + config.name + " (" + config.servletClass.getName() + ") to url "
                        + config.urlMapping);
                Dynamic uploadServlet = servletContext.addServlet(config.name, config.servletClass);
                uploadServlet.addMapping(config.urlMapping);
                uploadServlet.setMultipartConfig(multipartConfig);
            });
        }
    }

    private void checkTempStoragePath(String location) throws IOException {
        Path targetDir = Paths.get(location);
        if (!targetDir.isAbsolute()) {
            throw new MCRConfigurationException(
                "'" + UPLOAD_TEMP_STORAGE_PATH + "=" + location + "' must be an absolute path!");
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
        String location = MCRConfiguration2.getStringOrThrow(UPLOAD_TEMP_STORAGE_PATH);
        long maxFileSize = MCRConfiguration2.getOrThrow("MCR.SWORD.FileUpload.MaxSize", Long::parseLong);
        int fileSizeThreshold = MCRConfiguration2.getOrThrow("MCR.SWORD.FileUpload.MemoryThreshold",
            Integer::parseInt);
        LogManager.getLogger().info(
            () -> "mycore-sword accept files and requests up to " + maxFileSize + " bytes and uses " + location
                + " as tempory storage for files larger " + fileSizeThreshold + " bytes.");
        return new MultipartConfigElement(location, maxFileSize, maxFileSize, fileSizeThreshold);
    }

    private static class ServletConfig {
        String name, urlMapping;

        Class<? extends HttpServlet> servletClass;

        ServletConfig(String name, String urlMapping, Class<? extends HttpServlet> servletClass) {
            this.name = name;
            this.urlMapping = urlMapping;
            this.servletClass = servletClass;
        }
    }

}
