package org.mycore.ocfl.niofs;

import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRPath;

import java.nio.file.FileSystemException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MCROCFLVirtualObjectProviderTest extends MCROCFLNioTestCase {

    public MCROCFLVirtualObjectProviderTest(boolean remote) {
        super(remote);
    }

    @Test
    public void exists() throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        // check default
        assertTrue(virtualObjectProvider.exists(DERIVATE_1));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));

        // check after read map is filled
        assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));

        // check after write map is filled
        MCRTransactionHelper.beginTransaction();
        assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));
        fs.createRoot(DERIVATE_2);
        assertTrue(virtualObjectProvider.exists(DERIVATE_2));
        MCRTransactionHelper.commitTransaction();
        assertTrue(virtualObjectProvider.exists(DERIVATE_2));
    }

}
