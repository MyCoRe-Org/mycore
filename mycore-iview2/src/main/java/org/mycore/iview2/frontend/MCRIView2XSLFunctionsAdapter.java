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

package org.mycore.iview2.frontend;

import java.nio.file.Files;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * Adapter that can be extended to work with different internal files systems.
 * To get the extending class invoked, one need to define a MyCoRe property, which defaults to:
 * <code>MCR.Module-iview2.MCRIView2XSLFunctionsAdapter=org.mycore.iview2.frontend.MCRIView2XSLFunctionsAdapter</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2XSLFunctionsAdapter {

    public static MCRIView2XSLFunctionsAdapter obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public boolean hasMETSFile(String derivateID) {
        return Files.exists(MCRPath.getPath(derivateID, "/mets.xml"));
    }

    public String getSupportedMainFile(String derivateID) {
        return MCRIView2Tools.getSupportedMainFile(derivateID);
    }

    private static final class LazyInstanceHolder {
        public static final MCRIView2XSLFunctionsAdapter SHARED_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRIView2XSLFunctionsAdapter.class, MCRIView2Tools.CONFIG_PREFIX + "MCRIView2XSLFunctionsAdapter");
    }

}
