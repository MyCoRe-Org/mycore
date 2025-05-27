package org.mycore.ocfl.niofs;

import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLFileTypeDetectorTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void probeContentType() throws IOException {
        String contentType = Files.probeContentType(MCRPath.getPath(DERIVATE_1, "white.png"));
        Assertions.assertEquals("image/png", contentType, "white.png should be image/png");
    }

}
