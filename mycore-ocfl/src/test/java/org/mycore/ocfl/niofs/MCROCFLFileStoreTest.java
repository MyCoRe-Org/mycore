package org.mycore.ocfl.niofs;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLFileStoreTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @AfterEach
    public void tearDown() {
        MCROCFLFileSystemProvider.getMCROCFLFileSystem().resetFileStore();
    }

    @TestTemplate
    public void name() {
        Assertions.assertNotNull(fs().name(), "file store should have a name");
    }

    @TestTemplate
    public void type() {
        Assertions.assertNotNull(fs().type(), "file store should have a type");
    }

    @TestTemplate
    public void isReadOnly() {
        Assertions.assertFalse(fs().isReadOnly(), "file store shouldn't be readonly");
    }

    @TestTemplate
    public void space() throws IOException {
        if (!remote) {
            Assertions.assertNotEquals(0, fs().getTotalSpace(), "file store should have total space");
            Assertions.assertNotEquals(0, fs().getUsableSpace(), "file store should have usable space");
        }
    }

    private static MCROCFLFileStore fs() {
        return MCROCFLFileSystemProvider.getMCROCFLFileSystem().getFileStore();
    }

}
