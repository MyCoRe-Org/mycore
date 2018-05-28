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

package org.mycore.datamodel.ifs;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRContentStoreTestCase extends MCRJPATestCase {

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        testProperties.put("MCR.IFS.ContentStore.IFS2.BaseDir", storeBaseDir.getRoot().getAbsolutePath());
        return testProperties;
    }

    @Test
    public void delete() throws IOException {
        MCRObjectID derId = MCRObjectID.getInstance("MCR_derivate_00000001");
        MCRDirectory root = new MCRDirectory(derId.toString());
        MCRFile child = new MCRFile("empty.txt", root);
        child.setContentFrom(new byte[0]);
        File localFile = child.getLocalFile();
        Assert.assertEquals(0, localFile.length());
        startNewTransaction();
        MCRFile child2 = new MCRFile("empty2.txt", root);
        child2.setContentFrom(new byte[0]);
        File localFile2 = child2.getLocalFile();
        Assert.assertNotNull(localFile2);
        MCREntityManagerProvider.getCurrentEntityManager().getTransaction().rollback(); //error, see MCR-1634
        beginTransaction();
        root.delete();
        Assert.assertFalse(localFile.exists());
        Assert.assertFalse(localFile.getParentFile().exists());
        System.out.println(localFile.getAbsolutePath());
    }

}
