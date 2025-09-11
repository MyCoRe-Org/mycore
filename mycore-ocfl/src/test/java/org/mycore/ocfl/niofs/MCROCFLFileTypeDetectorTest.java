package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.WHITE_PNG;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
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
        assertEquals("image/png", Files.probeContentType(WHITE_PNG), "white.png should be image/png");

        MCRVersionedPath file1 = MCRVersionedPath.head(DERIVATE_1, "file1.txt");

        MCRTransactionManager.beginTransactions();
        assertEquals("image/png", Files.probeContentType(WHITE_PNG), "white.png should be image/png");
        Files.write(file1, new byte[] { 1 });
        assertEquals("text/plain", Files.probeContentType(file1), "file1.txt should be text/plain");
        MCRTransactionManager.commitTransactions();
        assertEquals("text/plain", Files.probeContentType(file1), "file1.txt should be text/plain");
    }

}
