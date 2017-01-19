/*
 * $Id$
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

package org.mycore.iview2.events;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.iview2.services.MCRImageTiler;

/**
 * Handles tiling process in a web application.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2TilingThreadStarter implements MCRStartupHandler.AutoExecutable {
    private static Logger LOGGER = LogManager.getLogger(MCRIView2TilingThreadStarter.class);

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
        if (servletContext != null && !MCRImageTiler.isRunning()) {
            LOGGER.info("Starting Tiling thread.");
            System.setProperty("java.awt.headless", "true");
            Thread tilingThread = new Thread(MCRImageTiler.getInstance());
            tilingThread.start();
        }
    }

}
