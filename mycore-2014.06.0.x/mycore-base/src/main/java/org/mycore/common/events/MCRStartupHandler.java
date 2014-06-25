/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 17, 2013 $
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

package org.mycore.common.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDirSetup;

/**
 * Initializes classes that implement {@link AutoExecutable} interface that are defined via <code>MCR.Startup.Class</code> property.
 * 
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRStartupHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRStartupHandler.class);

    public static interface AutoExecutable {
        /**
         * returns a name to display on start-up.
         * @return
         */
        public String getName();

        /**
         * If order is important returns as 'heigher' priority.
         * @return
         */
        public int getPriority();

        /**
         * This method get executed by {@link MCRStartupHandler#startUp()}
         * @param servletContext 
         */
        public void startUp(ServletContext servletContext);
    }

    private static class AutoExecutableComparator implements Comparator<AutoExecutable> {
        @Override
        public int compare(AutoExecutable o1, AutoExecutable o2) {
            //reverse ordering: highest priority first
            return Integer.compare(o2.getPriority(), o1.getPriority());
        }
    }

    public static void startUp(ServletContext servletContext) {
        //setup configuration
        MCRConfigurationDirSetup dirSetup = new MCRConfigurationDirSetup();
        dirSetup.startUp(servletContext);

        List<String> startupClasses = MCRConfiguration.instance().getStrings("MCR.Startup.Class", null);
        if (startupClasses == null) {
            return;
        }

        List<AutoExecutable> autoExecutables = new ArrayList<AutoExecutable>(startupClasses.size());
        Collections.sort(autoExecutables, new AutoExecutableComparator());

        for (String className : startupClasses) {
            try {
                AutoExecutable autoExecutable = (AutoExecutable) Class.forName(className).newInstance();
                boolean added = autoExecutables.add(autoExecutable);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                LOGGER.error("Error while starting startup class: " + className, e);
            }
        }
        for (AutoExecutable autoExecutable : autoExecutables) {
            LOGGER.info(autoExecutable.getPriority() + ": Starting " + autoExecutable.getName());
            autoExecutable.startUp(servletContext);
        }
    }
}
