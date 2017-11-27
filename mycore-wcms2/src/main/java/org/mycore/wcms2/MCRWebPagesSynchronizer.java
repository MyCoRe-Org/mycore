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

package org.mycore.wcms2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRWebPagesSynchronizer implements AutoExecutable {

    private static Logger LOGGER = LogManager.getLogger(MCRWebPagesSynchronizer.class);

    private static final int FAT_PRECISION = 2000;

    private static final long DEFAULT_COPY_BUFFER_SIZE = 16 * 1024 * 1024; // 16 KB

    private static ServletContext SERVLET_CONTEXT = null;

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return "Webpages synchronizer";
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
            SERVLET_CONTEXT = servletContext;
            File wcmsDataDir = null, webappBasePath = null;
            try {
                //we are running in a servlet container
                webappBasePath = getWebAppBaseDir();
                LOGGER.info("WebAppBasePath={}", webappBasePath.getAbsolutePath());
                wcmsDataDir = getWCMSDataDir();
                if (!wcmsDataDir.isDirectory()) {
                    LOGGER.info("{} does not exist or is not a directory. Skipping synchronization.",
                        wcmsDataDir.getAbsolutePath());
                    return;
                }
                synchronize(wcmsDataDir, webappBasePath);
            } catch (IOException e) {
                throw new MCRException("Error while synchronizing " + wcmsDataDir + " to " + webappBasePath, e);
            }
        }
    }

    public static File getWCMSDataDir() {
        return new File(MCRConfiguration.instance().getString("MCR.WCMS2.DataDir"));
    }

    public static File getWebAppBaseDir() throws IOException {
        if (SERVLET_CONTEXT == null) {
            throw new IOException("ServletContext is not initialized.");
        }
        String realPath = SERVLET_CONTEXT.getRealPath("/");
        if (realPath == null) {
            throw new IOException("Could not get webapp base path.");
        }
        return new File(realPath);
    }

    /**
     * Returns an OuputStream that writes to local webapp and to file inside <code>MCR.WCMS2.DataDir</code>.
     */
    public static OutputStream getOutputStream(String path) throws IOException {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        File wcmsDataDir = getWCMSDataDir();
        File webappBaseDir = getWebAppBaseDir();
        File webappTarget = new File(webappBaseDir, cleanPath);
        if (!webappTarget.toPath().startsWith(webappBaseDir.toPath())) {
            throw new IOException(String.format(Locale.ROOT, "Cannot write %s outside the web application: %s",
                webappTarget, webappBaseDir));
        }
        File wcmsDataDirTarget = new File(wcmsDataDir, cleanPath);
        createDirectoryIfNeeded(webappTarget);
        createDirectoryIfNeeded(wcmsDataDirTarget);
        LOGGER.info(String.format(Locale.ROOT, "Writing content to %s and to %s.", webappTarget, wcmsDataDirTarget));
        return new TeeOutputStream(new FileOutputStream(wcmsDataDirTarget), new FileOutputStream(webappTarget));
    }

    private static void createDirectoryIfNeeded(File targetFile) throws IOException {
        File targetDirectory = targetFile.getParentFile();
        if (!targetDirectory.isDirectory()) {
            if (!targetDirectory.mkdirs()) {
                throw new IOException(String.format(Locale.ROOT, "Could not create directory: %s", targetDirectory));
            }
        }
    }

    /**
     * Returns URL of the given resource.
     * 
     * This URL may point to a file inside a JAR file in <code>WEB-INF/lib</code>.
     * @param path should start with '/'
     * @return null, if no resource with that path could be found
     */
    public static URL getURL(String path) throws MalformedURLException {
        String cleanPath = path.startsWith("/") ? path : String.format(Locale.ROOT, "/%s", path);
        return SERVLET_CONTEXT.getResource(cleanPath);
    }

    /**
     * Returns an InputStream of the given resource.
     * 
     * @param path should start with '/'
     * @return null, if no resource with that path could be found
     */
    public InputStream getInputStream(String path) {
        String cleanPath = path.startsWith("/") ? path : String.format(Locale.ROOT, "/%s", path);
        return SERVLET_CONTEXT.getResourceAsStream(cleanPath);
    }

    private static void synchronize(File wcmsDataDir, File webappBasePath) throws IOException {
        synchronize(wcmsDataDir, webappBasePath, DEFAULT_COPY_BUFFER_SIZE);
    }

    private static void synchronize(File source, File destination, long chunkSize) throws IOException {
        if (chunkSize <= 0) {
            LOGGER.error("Chunk size must be positive: using default value.");
            chunkSize = DEFAULT_COPY_BUFFER_SIZE;
        }
        if (source.isDirectory()) {
            if (!destination.exists()) {
                if (!destination.mkdirs()) {
                    throw new IOException("Could not create path " + destination);
                }
            } else if (!destination.isDirectory()) {
                throw new IOException("Source and Destination not of the same type:" + source.getCanonicalPath()
                    + " , " + destination.getCanonicalPath());
            }
            File[] sources = source.listFiles();

            //copy each file from source
            for (File srcFile : sources) {
                File destFile = new File(destination, srcFile.getName());
                synchronize(srcFile, destFile, chunkSize);
            }
        } else {
            if (destination.exists() && destination.isDirectory()) {
                delete(destination);
            }
            if (destination.exists()) {
                long sts = source.lastModified() / FAT_PRECISION;
                long dts = destination.lastModified() / FAT_PRECISION;
                //do not copy same timestamp and same length
                if (sts == 0 || sts != dts || source.length() != destination.length()) {
                    copyFile(source, destination, chunkSize);
                }
            } else {
                copyFile(source, destination, chunkSize);
            }
        }
    }

    private static void copyFile(File srcFile, File destFile, long chunkSize) throws IOException {
        try (FileInputStream is = new FileInputStream(srcFile);
            FileOutputStream os = new FileOutputStream(destFile, false)) {
            FileChannel iChannel = is.getChannel();
            FileChannel oChannel = os.getChannel();
            long doneBytes = 0L;
            long todoBytes = srcFile.length();
            while (todoBytes != 0L) {
                long iterationBytes = Math.min(todoBytes, chunkSize);
                long transferredLength = oChannel.transferFrom(iChannel, doneBytes, iterationBytes);
                if (iterationBytes != transferredLength) {
                    throw new IOException("Error during file transfer: expected " + iterationBytes + " bytes, only "
                        + transferredLength + " bytes copied.");
                }
                doneBytes += transferredLength;
                todoBytes -= transferredLength;
            }
        }
        boolean successTimestampOp = destFile.setLastModified(srcFile.lastModified());
        if (!successTimestampOp) {
            LOGGER.warn(String.format(Locale.ROOT,
                "Could not change timestamp for %s. Index synchronization may be slow.", destFile));
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        if (file.exists()) {
            if (!file.delete()) {
                LOGGER.warn(String.format(Locale.ROOT, "Could not delete %s.", file));
            }
        }
    }

}
