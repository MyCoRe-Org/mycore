/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 8, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.wcms2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRWebPagesSynchronizer implements AutoExecutable {

    private static Logger LOGGER = Logger.getLogger(MCRWebPagesSynchronizer.class);

    private static final int FAT_PRECISION = 2000;

    private static final long DEFAULT_COPY_BUFFER_SIZE = 16 * 1024 * 1024; // 16 KB

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
            //we are running in a servlet container
            String realPath = servletContext.getRealPath("/");
            if (realPath == null) {
                LOGGER.warn("Could not get webapp base path.");
                return;
            }
            File webappBasePath = new File(realPath);
            LOGGER.info("WebAppBasePath=" + webappBasePath.getAbsolutePath());
            File wcmsDataDir = new File(MCRConfiguration.instance().getString("MCR.WCMS2.DataDir"));
            if (!wcmsDataDir.isDirectory()) {
                LOGGER.info(wcmsDataDir.getAbsolutePath()
                    + " does not exist or is not a directory. Skipping synchronization.");
                return;
            }
            try {
                synchronize(wcmsDataDir, webappBasePath);
            } catch (IOException e) {
                throw new MCRException("Error while synchronizing " + wcmsDataDir + " to " + webappBasePath, e);
            }
        }
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
            LOGGER.warn(String
                .format("Could not change timestamp for %s. Index synchronization may be slow.", destFile));
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
                LOGGER.warn(String.format("Could not delete %s.", file));
            }
        }
    }

}
