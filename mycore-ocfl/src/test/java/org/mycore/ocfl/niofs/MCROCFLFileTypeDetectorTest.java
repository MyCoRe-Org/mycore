package org.mycore.ocfl.niofs;

import org.junit.Test;
import org.mycore.datamodel.niofs.MCRPath;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class MCROCFLFileTypeDetectorTest extends MCROCFLNioTestCase {

    public MCROCFLFileTypeDetectorTest(boolean remote) {
        super(remote);
    }

    @Test
    public void probeContentType() throws IOException {
        String contentType = Files.probeContentType(MCRPath.getPath(DERIVATE_1, "white.png"));
        assertEquals("white.png should be image/png", "image/png", contentType);
    }

}
