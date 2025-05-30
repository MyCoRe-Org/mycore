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

package org.mycore.iview2.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.iview2.services.MCRImageTiler;

import jakarta.servlet.ServletContext;

/**
 * Handles tiling process in a web application.
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRIView2TilingThreadStarter implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRIView2TilingThreadStarter SINGLETON_INSTANCE = new MCRIView2TilingThreadStarter();

    private boolean started;

    private MCRIView2TilingThreadStarter(){
    }

    public static MCRIView2TilingThreadStarter getInstance(){
        return SINGLETON_INSTANCE;
    }

    @Override
    public String getName() {
        return "Image Viewer Tiling Thread";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            startMasterTilingThread();
        }
    }

    public synchronized void startMasterTilingThread() {
        if (!started) {
            LOGGER.info("Starting Tiling thread.");
            System.setProperty("java.awt.headless", "true");
            Thread tilingThread = new Thread(MCRImageTiler.getInstance());
            tilingThread.start();
            started = true;
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }

}
