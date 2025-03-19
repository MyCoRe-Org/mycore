package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.mycore.datamodel.niofs.MCRPath;

public class MCROCFLFileTypeDetectorTest extends MCROCFLNioTestCase {

    public MCROCFLFileTypeDetectorTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Test
    public void probeContentType() throws IOException {
        String contentType = Files.probeContentType(MCRPath.getPath(DERIVATE_1, "white.png"));
        assertEquals("white.png should be image/png", "image/png", contentType);
    }

}
