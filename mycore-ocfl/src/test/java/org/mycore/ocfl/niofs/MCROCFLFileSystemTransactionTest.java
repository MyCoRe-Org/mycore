package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

public class MCROCFLFileSystemTransactionTest extends MCROCFLNioTestCase {

    public MCROCFLFileSystemTransactionTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Test
    public void commit() throws IOException {
        MCRVersionedPath whitePng = MCRVersionedPath.head(DERIVATE_1, "white.png");
        MCRVersionedPath other = MCRVersionedPath.head(DERIVATE_1, "other");

        // no write operation: version should stay the same
        checkVersion(1);
        MCRTransactionManager.beginTransactions();
        getWritable();
        checkVersion(1);
        MCRTransactionManager.commitTransactions();
        checkVersion(1);

        // delete operation: version should increase
        MCRTransactionManager.beginTransactions();
        MCROCFLVirtualObject virtualObject = getWritable();
        virtualObject.delete(whitePng);
        checkVersion(1);
        MCRTransactionManager.commitTransactions();
        checkVersion(2);

        // write operation: version should increase
        MCRTransactionManager.beginTransactions();
        virtualObject = getWritable();
        checkVersion(2);
        Set<StandardOpenOption> options = Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        try (ByteChannel channel = virtualObject.newByteChannel(other, options)) {
            channel.write(ByteBuffer.wrap(new byte[] { 1 }));
        }
        checkVersion(2);
        MCRTransactionManager.commitTransactions();
        checkVersion(3);

        // write operation with the same content: version should not increase
        MCRTransactionManager.beginTransactions();
        virtualObject = getWritable();
        try (ByteChannel channel = virtualObject.newByteChannel(other, Set.of(StandardOpenOption.WRITE))) {
            channel.write(ByteBuffer.wrap(new byte[] { 1 }));
        }
        MCRTransactionManager.commitTransactions();
        checkVersion(3);
    }

    private static MCROCFLVirtualObject getWritable() {
        return MCROCFLFileSystemProvider.get().virtualObjectProvider().getWritable(DERIVATE_1);
    }

    private void checkVersion(int version) {
        OcflObjectVersion derivate1 = repository.getObject(ObjectVersionId.head(DERIVATE_1_OBJECT_ID));
        Assert.assertEquals("derivate version is wrong", version, derivate1.getVersionNum().getVersionNum());
    }

}
