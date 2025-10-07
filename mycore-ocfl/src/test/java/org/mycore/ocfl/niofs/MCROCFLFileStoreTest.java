package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
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
        assertNotNull(fs().name(), "file store should have a name");
    }

    @TestTemplate
    public void type() {
        assertNotNull(fs().type(), "file store should have a type");
    }

    @TestTemplate
    public void isReadOnly() {
        assertFalse(fs().isReadOnly(), "file store shouldn't be readonly");
    }

    @TestTemplate
    public void space() throws IOException {
        if (!remote) {
            assertNotEquals(0, fs().getTotalSpace(), "file store should have total space");
            assertNotEquals(0, fs().getUsableSpace(), "file store should have usable space");
        }
    }

    private static MCROCFLFileStore fs() {
        return MCROCFLFileSystemProvider.getMCROCFLFileSystem().getFileStore();
    }

}
