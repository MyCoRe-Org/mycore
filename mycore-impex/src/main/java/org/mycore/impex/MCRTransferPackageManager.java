package org.mycore.impex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;

/**
 * Singleton instance for managing transfer packages. You can call {@link #schedule(MCRTransferPackage)}
 * to add transfer packages.
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRTransferPackageManager {

    private static final Logger LOGGER = LogManager.getLogger(MCRTransferPackageManager.class);

    /**
     * The singleton instance
     */
    private static MCRTransferPackageManager INSTANCE;

    private static Path SAVE_DIRECTORY_PATH;

    private ExecutorService executorService;

    static {
        String alternative = MCRConfiguration.instance().getString("MCR.datadir") + File.separator
            + "transferPackages";
        String directoryPath = MCRConfiguration.instance().getString("MCR.TransferPackage.Save.to.Directory", alternative);
        SAVE_DIRECTORY_PATH = Paths.get(directoryPath);
    }

    /**
     * Hidden standard constructor.
     */
    private MCRTransferPackageManager() {
        super();
        executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * @return the singleton instance of this class
     */
    public static MCRTransferPackageManager instance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (MCRTransferPackageManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new MCRTransferPackageManager();
            }
            return INSTANCE;
        }
    }

    /**
     * Adds the given {@link TransferPackage} to the list of tasks needing
     * execution.
     * 
     * @param tp
     *            the {@link TransferPackage} to schedule
     */
    public void schedule(MCRTransferPackage tp) {
        LOGGER.info("Scheduling TransferPackage " + tp);
        LOGGER.info("Transfer Package will be stored at " + getTarPath(tp).toAbsolutePath().toString());
        executorService.execute(new RunnableTransferPackage(tp));
    }

    /**
     * Returns the path to the tar archive where the transfer package will be stored.
     * 
     * @param tp the transfer package
     * @return path to the *.tar location
     */
    public static Path getTarPath(MCRTransferPackage tp) {
        return SAVE_DIRECTORY_PATH.resolve(tp.getSource().getId() + ".tar");
    }

    private static class RunnableTransferPackage implements Runnable {
        private static final Logger LOGGER = LogManager.getLogger(RunnableTransferPackage.class);

        private MCRTransferPackage tp;

        /**
         * @param tp
         *            the TransferPackage to create
         */
        public RunnableTransferPackage(MCRTransferPackage tp) {
            this.tp = tp;
        }

        @Override
        public void run() {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            try {
                currentSession.beginTransaction();
                // build the package
                tp.build();
                // check root directory
                checkAndCreateSaveDirectory();
                // create the .tar
                buildTar(getTarPath(tp));
            } catch (Exception ex) {
                LOGGER.error("Error creating TransferPackage " + tp, ex);
            } finally {
                currentSession.commitTransaction();
                String msg = MessageFormat.format(
                    "Creating TransferPackage for id \"{0}\" took approximately {1} seconds", tp.getSource().getId(),
                    (int) Math.ceil(stopWatch.getTime() / 1000));
                LOGGER.info(msg);
            }
        }

        private void buildTar(Path pathToTar) throws IOException {
            FileOutputStream fos = new FileOutputStream(pathToTar.toFile());
            try (TarArchiveOutputStream tarOutStream = new TarArchiveOutputStream(fos)) {
                for (Entry<String, MCRContent> contentEntry : tp.getContent().entrySet()) {
                    String filePath = contentEntry.getKey();
                    byte[] data = contentEntry.getValue().asByteArray();
                    if(LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding '" + filePath + "' to " + pathToTar.toAbsolutePath().toString());
                    }
                    writeFile(tarOutStream, filePath, data);
                    writeMD5(tarOutStream, filePath + ".md5", data);
                }
            }
        }

        private byte[] writeFile(TarArchiveOutputStream tarOutStream, String fileName, byte[] data) throws IOException {
            TarArchiveEntry tarEntry = new TarArchiveEntry(fileName);
            tarEntry.setSize(data.length);
            tarOutStream.putArchiveEntry(tarEntry);
            tarOutStream.write(data);
            tarOutStream.closeArchiveEntry();
            return data;
        }

        private void writeMD5(TarArchiveOutputStream tarOutStream, String fileName, byte[] data) throws IOException {
            String md5 = MCRUtils.getMD5Sum(new ByteArrayInputStream(data));
            byte[] md5Bytes = md5.getBytes(StandardCharsets.ISO_8859_1);
            writeFile(tarOutStream, fileName, md5Bytes);
        }

        private void checkAndCreateSaveDirectory() throws IOException {
            if(!Files.exists(SAVE_DIRECTORY_PATH)) {
                LOGGER.info("Creating directory " + SAVE_DIRECTORY_PATH.toAbsolutePath().toString());
                Files.createDirectories(SAVE_DIRECTORY_PATH);
            }
        }
    }
}
