package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class MCROCFLFileStoreTest extends MCROCFLNioTestCase {

    public MCROCFLFileStoreTest(boolean remote) {
        super(remote);
    }

    @Override
    public void tearDown() throws Exception {
        MCROCFLFileSystemProvider.getMCROCFLFileSystem().resetFileStore();
        super.tearDown();
    }

    @Test
    public void name() {
        assertNotNull("file store should have a name", fs().name());
    }

    @Test
    public void type() {
        assertNotNull("file store should have a type", fs().type());
    }

    @Test
    public void isReadOnly() {
        assertFalse("file store shouldn't be readonly", fs().isReadOnly());
    }

    @Test
    public void space() throws IOException {
        assertNotEquals("file store should have total space", 0, fs().getTotalSpace());
        assertNotEquals("file store should have unallocated space", 0, fs().getUnallocatedSpace());
        assertNotEquals("file store should have usable space", 0, fs().getUsableSpace());
    }

    private static MCROCFLFileStore fs() {
        return MCROCFLFileSystemProvider.getMCROCFLFileSystem().getFileStore();
    }

}
