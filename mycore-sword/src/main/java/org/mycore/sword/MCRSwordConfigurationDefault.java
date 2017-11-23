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

import org.mycore.common.config.MCRConfiguration;
import org.swordapp.server.SwordConfiguration;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordConfigurationDefault implements SwordConfiguration {
    @Override
    public boolean returnDepositReceipt() {
        return true;
    }

    @Override
    public boolean returnStackTraceInError() {
        return true;
    }

    @Override
    public boolean returnErrorBody() {
        return true;
    }

    @Override
    public String generator() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Generator.Content");
    }

    @Override
    public String generatorVersion() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Generator.Version");
    }

    @Override
    public String administratorEmail() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Administrator.Mail");
    }

    @Override
    public String getAuthType() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Auth.Method", "Basic");
    }

    @Override
    public boolean storeAndCheckBinary() {
        return false; // MCR code stores files and checks files..
    }

    @Override
    public String getTempDirectory() {
        return MCRConfiguration.instance().getString("MCR.SWORD.TempDirectory", System.getProperty("java.io.tmpdir"));
    }

    @Override
    public long getMaxUploadSize() {
        return MCRConfiguration.instance().getLong("MCR.SWORD.Max.Uploaded.File.Size");
    }

    @Override
    public String getAlternateUrl() {
        return null;
    }

    @Override
    public String getAlternateUrlContentType() {
        return null;
    }

    @Override
    public boolean allowUnauthenticatedMediaAccess() {
        return false;
    }
}
