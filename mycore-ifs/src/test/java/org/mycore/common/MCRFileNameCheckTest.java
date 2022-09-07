package org.mycore.common;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRFileNameCheckTest extends MCRIFSTest {

    private MCRObject root;

    private MCRDerivate derivate;

    @Before
    public void setup() throws MCRAccessException {
        root = createObject();
        derivate = createDerivate(root.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @Test(expected = IOException.class)
    public void checkIllegalWindowsFileName() throws MCRAccessException, IOException {
        final MCRPath aux = MCRPath.getPath(derivate.toString(), "aux");
        Files.createFile(aux);
    }

    @Test(expected = IOException.class)
    public void checkIllegalFileName() throws MCRAccessException, IOException {
        final MCRPath info = MCRPath.getPath(derivate.toString(), "info@mycore.de");
        Files.createFile(info);
    }

    @Test(expected = IOException.class)
    public void checkIllegalDirectoryName() throws MCRAccessException, IOException {
        final MCRPath dirName = MCRPath.getPath(derivate.toString(), "Nur ein \"Test\"");
        Files.createDirectory(dirName);
    }

}
