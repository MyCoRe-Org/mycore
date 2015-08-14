/*
 * 
 * $Revision$ $Date$
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
package org.mycore.iview2.remoteclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.remoteclient.MCRIView2RemoteFunctions;
import org.mycore.iview2.services.remoteclient.MCRIView2RemoteFunctionsService;
import org.mycore.iview2.services.remoteclient.MCRIView2RemoteJob;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@SuppressWarnings("restriction")
public class MCRIView2Client {

    private static Logger LOGGER = Logger.getLogger(MCRIView2Client.class);

    private static boolean finished = false;

    private static ExecutorService EXECUTOR_SERVICE;

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            printHelp();
            return;
        }
        LOGGER.info("Activating headless mode");
        System.setProperty("java.awt.headless", "true");
        String endPoint = args[0];
        final Path fileStoreDir = Paths.get(System.getProperty("fileStoreDir", "filestore"));
        if (!Files.isDirectory(fileStoreDir) && !Files.isReadable(fileStoreDir)) {
            LOGGER.error("Cannot access fileStoreDir: " + fileStoreDir.toAbsolutePath());
            return;
        }
        final Path tileDir = Paths.get(System.getProperty("tileDir", "iview2/tiles"));
        if (!Files.exists(tileDir)) {
            LOGGER.warn("Creating tileDir: " + tileDir.toAbsolutePath());
            Files.createDirectories(tileDir);
        }
        int threadCount = Integer.parseInt(System.getProperty("tileThreads", "1"));
        LOGGER.info("Starting ExecutorService with " + threadCount + " Threads");
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "TileSlave #" + counter.incrementAndGet());
                return thread;
            }
        });
        LOGGER.info("Install SIGINT handler");
        MySignalHandler.install("INT");
        boolean skipExisting = System.getProperty("skipExisting") != null;

        LOGGER.info("Connecting to WebService on " + endPoint);
        for (int i = 0; i < threadCount; i++) {
            final MCRIView2RemoteFunctions iView2RemoteFunctions = getIViewRemoteFunctions(endPoint);
            EXECUTOR_SERVICE.submit(new ImageTiler(iView2RemoteFunctions, tileDir, fileStoreDir, skipExisting));
        }
        while (!EXECUTOR_SERVICE.isTerminated()) {
            Thread.yield();
        }
        finished = true;
        LOGGER.info("No more jobs in queue, exiting.");
    }

    private static void printHelp() {
        System.out.println("Usage:\n");
        System.out.println("java <options> org.mycore.iview2.remoteclient.MCRIView2Client <endpoint>\n");
        System.out.println("    endpoint: WebService endpoint for IView2\n");
        System.out.println("Options:");
        System.out.println("              -DskipExisting: Whether to skip existing *.iview2 files");
        System.out.println("             -DtileDir=<dir>: Directory where to save tiles, defaults to './iview2/tiles'");
        System.out.println("        -DfileStoreDir=<dir>: Where to get the images from, defaults to './filestore'");
        System.out.println(" -DtileThreads=<threadCount>: How many threads should run in parallel, defaults to '1'");
    }

    private static MCRIView2RemoteFunctions getIViewRemoteFunctions(String endPoint) throws MalformedURLException {
        QName qName = new QName("http://mycore.org/iview2/services/remoteClient", "MCRIView2RemoteFunctionsService");
        URL serviceURL = new URL(endPoint + "?wsdl");
        MCRIView2RemoteFunctionsService service = new MCRIView2RemoteFunctionsService(serviceURL, qName);
        MCRIView2RemoteFunctions iView2RemoteFunctions = service.getMCRIView2RemoteFunctionsPort();
        ((BindingProvider) iView2RemoteFunctions).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            endPoint);
        return iView2RemoteFunctions;
    }

    private static class ImageTiler implements Callable<MCRIView2RemoteJob> {
        Path tileDir;

        Path fileStoreDir;

        MCRIView2RemoteFunctions iView2RemoteFunctions;

        private boolean skipExisting;

        public ImageTiler(MCRIView2RemoteFunctions iView2RemoteFunctions, Path tileDir, Path fileStoreDir,
            boolean skipExisting) {
            super();
            this.iView2RemoteFunctions = iView2RemoteFunctions;
            this.tileDir = tileDir;
            this.fileStoreDir = fileStoreDir;
            this.skipExisting = skipExisting;
            LOGGER.info("ImageTile initialized");
        }

        @Override
        public MCRIView2RemoteJob call() throws Exception {
            LOGGER.info("Thread started " + iView2RemoteFunctions);
            MCRIView2RemoteJob tileJob = iView2RemoteFunctions.nextTileJob();
            LOGGER.info("Got next tile job " + tileJob);
            if (tileJob.getDerivateID() != null) {
                try {
                    if (!handleTileJob(tileJob, tileDir, fileStoreDir, skipExisting))
                        return null;
                    iView2RemoteFunctions.finishTileJob(tileJob);
                    return tileJob;
                } catch (Error e) {
                    LOGGER.error("Error while tiling " + tileJob.getDerivateID() + tileJob.getDerivatePath(), e);
                    throw e;
                } catch (Exception e) {
                    LOGGER.error("Exception while tiling " + tileJob.getDerivateID() + tileJob.getDerivatePath(), e);
                    throw e;
                } finally {
                    if (!EXECUTOR_SERVICE.isShutdown())
                        EXECUTOR_SERVICE.submit(this);
                }
            }
            EXECUTOR_SERVICE.shutdown();
            return null;
        }

        private static boolean handleTileJob(MCRIView2RemoteJob tileJob, Path tileDir, Path fileStoreDir,
            boolean skipExisting) throws IOException, JDOMException {

            LOGGER.info("Tiling " + tileJob.getDerivateID() + tileJob.getDerivatePath());
            Path tiledFile = MCRImage.getTiledFile(tileDir, tileJob.getDerivateID(), tileJob.getDerivatePath());
            MCRTiledPictureProps tiledPictureProps;
            if (skipExisting && Files.exists(tiledFile)) {
                tiledPictureProps = MCRTiledPictureProps.getInstanceFromFile(tiledFile);
            } else {
                String fileSystemPath = tileJob.getFileSystemPath();
                if (!fileStoreDir.getFileSystem().getSeparator().equals("/")) {
                    fileSystemPath = fileSystemPath.replace("/", fileStoreDir.getFileSystem().getSeparator());
                }
                MCRImage mcrImage = MCRImage.getInstance(fileStoreDir.resolve(fileSystemPath), tileJob.getDerivateID(),
                    tileJob.getDerivatePath());
                mcrImage.setTileDir(tileDir);
                tiledPictureProps = mcrImage.tile();
            }
            tileJob.setHeight(tiledPictureProps.getHeight());
            tileJob.setWidth(tiledPictureProps.getWidth());
            tileJob.setZoomLevel(tiledPictureProps.getZoomlevel());
            tileJob.setTiles(tiledPictureProps.getTilesCount());
            return true;
        }

    }

    private static class MySignalHandler implements SignalHandler {

        private SignalHandler oldHandler;

        public static MySignalHandler install(String signalName) {
            Signal signal = new Signal(signalName);
            MySignalHandler signalHandler = new MySignalHandler();
            signalHandler.oldHandler = Signal.handle(signal, signalHandler);
            return signalHandler;
        }

        @Override
        public void handle(Signal signalName) {
            LOGGER.info("Cought signal: " + signalName);
            try {
                EXECUTOR_SERVICE.shutdown();
                while (!finished)
                    Thread.yield();
                LOGGER.info("Calling old SignalHandler: " + oldHandler);
                oldHandler.handle(signalName);
            } catch (Exception e) {
                LOGGER.error("Signal handler failed.", e);
            }
        }

    }

}
