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

package org.mycore.ocfl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRDefaultConfigurationLoader;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLTestUtil {

    @ClassRule
    public static TemporaryFolder junitFolder = new TemporaryFolder();

    private static MCRConfigurationLoader loader = new MCRDefaultConfigurationLoader();

    @BeforeClass
    public static void initConfig() throws IOException {
        LogManager.getLogger().info(junitFolder.getRoot().getAbsolutePath());

        MCRConfigurationBase.initialize(loader.loadDeprecated(), loader.load(), false);

        MCRConfiguration2.set("MCR.OCFL.Util.ExportDir", junitFolder.newFolder("ocfl-export").getAbsolutePath());
        MCRConfiguration2.set("MCR.OCFL.Util.BackupDir", junitFolder.newFolder("ocfl-backup").getAbsolutePath());
        MCRConfiguration2.set("MCR.OCFL.Repository.Adapt.RepositoryRoot",
            junitFolder.newFolder("ocfl-adapt").getAbsolutePath());
        MCRConfiguration2.set("MCR.OCFL.Repository.Adapt.WorkDir",
            junitFolder.newFolder("ocfl-temp").getAbsolutePath());
        MCRConfiguration2.set("MCR.OCFL.Repository.JUnit.RepositoryRoot",
            junitFolder.newFolder("ocfl-root").getAbsolutePath());
        MCRConfiguration2.set("MCR.OCFL.Repository.JUnit.WorkDir",
            MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.WorkDir"));

        MCRConfiguration2.set("MCR.Metadata.ObjectID.NumberPattern",
            MCRConfiguration2.getStringOrThrow("MCR.OCFL.TestBase.NumberPattern"));
        MCRConfiguration2.set("MCR.IFS2.Store.class.SlotLayout",
            MCRConfiguration2.getStringOrThrow("MCR.Metadata.ObjectID.NumberPattern").length() - 4 + "-2-2");
    }

}
