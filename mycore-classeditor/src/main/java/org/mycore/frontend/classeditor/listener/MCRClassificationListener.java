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

package org.mycore.frontend.classeditor.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.ext.Provider;

import org.mycore.common.MCRJSONManager;
import org.mycore.frontend.classeditor.MCRCategoryIDTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryListTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryTypeAdapter;
import org.mycore.frontend.classeditor.MCRLabelSetTypeAdapter;

@Provider
public class MCRClassificationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        MCRJSONManager mg = MCRJSONManager.instance();
        mg.registerAdapter(new MCRCategoryTypeAdapter());
        mg.registerAdapter(new MCRCategoryIDTypeAdapter());
        mg.registerAdapter(new MCRLabelSetTypeAdapter());
        mg.registerAdapter(new MCRCategoryListTypeAdapter());
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        //do nothing
    }

}
