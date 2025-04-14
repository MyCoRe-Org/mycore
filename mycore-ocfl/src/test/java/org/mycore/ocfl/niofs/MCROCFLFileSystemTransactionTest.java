package org.mycore.ocfl.niofs;

import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1_OBJECT_ID;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLFileSystemTransactionTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
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
        Assertions.assertEquals(version, derivate1.getVersionNum().getVersionNum(), "derivate version is wrong");
    }

}
