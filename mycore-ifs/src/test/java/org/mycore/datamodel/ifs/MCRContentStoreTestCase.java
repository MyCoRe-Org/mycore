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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.ifs2.MCRCStoreIFS2;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRStoreCenter;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.MCRIFSCommands;

public class MCRContentStoreTestCase extends MCRJPATestCase {

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    @Before
    @After
    public void clearStores() {
        MCRContentStoreFactory.getAvailableStores().clear();
        MCRStoreCenter.instance().clear();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        testProperties.put("MCR.IFS.ContentStore.IFS2.BaseDir", storeBaseDir.getRoot().getAbsolutePath());
        testProperties.put("MCR.datadir", "%MCR.basedir%/data");
        testProperties.put("MCR.NIO.DefaultScheme", "ifs"); //required for testMD5CopyCommand()
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

    @Test
    public void md5Sum() throws IOException {
        MCRObjectID derId = MCRObjectID.getInstance("MCR_derivate_00000002");
        String fileName = "hallo.txt";
        MCRPath filePath = MCRPath.getPath(derId.toString(), fileName);
        Files.createFile(filePath);
        try (InputStream is = new CharSequenceInputStream("Hello World!", StandardCharsets.UTF_8)) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        startNewTransaction();
        MCRFileAttributes attrs = Files.readAttributes(filePath, MCRFileAttributes.class);
        MCRCStoreIFS2 ifs2 = (MCRCStoreIFS2) MCRContentStoreFactory.getStore("IFS2");
        MCRFileCollection fileCollection = ifs2.getIFS2FileCollection(derId);
        org.mycore.datamodel.ifs2.MCRFile file2 = (org.mycore.datamodel.ifs2.MCRFile) fileCollection.getChild(fileName);
        Assert.assertEquals("MD5 mismatch.", attrs.md5sum(), file2.getMD5());
    }

    @Test
    public void testMD5CopyCommand() throws IOException {
        MCRObjectID derId = MCRObjectID.getInstance("MCR_derivate_00000003");
        String fileName = "hallo.txt";
        MCRPath filePath = MCRPath.getPath(derId.toString(), fileName);
        Files.createFile(filePath);
        try (InputStream is = new CharSequenceInputStream("Hello World!", StandardCharsets.UTF_8)) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        startNewTransaction();
        MCRFileAttributes attrs = Files.readAttributes(filePath, MCRFileAttributes.class);
        MCRCStoreIFS2 ifs2 = (MCRCStoreIFS2) MCRContentStoreFactory.getStore("IFS2");
        MCRFileCollection fileCollection = ifs2.getIFS2FileCollection(derId);
        org.mycore.datamodel.ifs2.MCRFile file2 = (org.mycore.datamodel.ifs2.MCRFile) fileCollection.getChild(fileName);
        Assert.assertEquals("MD5 mismatch.", attrs.md5sum(), file2.getMD5());
        file2.setMD5("invalid");
        file2 = (org.mycore.datamodel.ifs2.MCRFile) fileCollection.getChild(fileName);
        Assert.assertNotEquals("MD5 was not updated.", attrs.md5sum(), file2.getMD5());
        MCRIFSCommands.copyMD5ToIFS2();
        file2 = (org.mycore.datamodel.ifs2.MCRFile) fileCollection.getChild(fileName);
        Assert.assertEquals("MD5 mismatch.", attrs.md5sum(), file2.getMD5());
    }

}
