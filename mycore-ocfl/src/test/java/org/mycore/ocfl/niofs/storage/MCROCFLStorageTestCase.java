package org.mycore.ocfl.niofs.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.MCROCFLNioTestCase;

public abstract class MCROCFLStorageTestCase extends MCROCFLNioTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected Path rootPath;

    protected MCRVersionedPath path1;

    protected MCRVersionedPath path2;

    protected MCRVersionedPath path3;

    public MCROCFLStorageTestCase(boolean remote) {
        super(remote);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.rootPath = folder.newFolder("ocfl-rolling-cache").toPath();
        path1 = MCRVersionedPath.head("owner1", "file1");
        path2 = MCRVersionedPath.head("owner1", "file2");
        path3 = MCRVersionedPath.head("owner1", "file3");
    }

    protected abstract MCROCFLTempFileStorage getStorage();

    protected void write(MCRVersionedPath path) throws IOException {
        write(path, new byte[] { 1 });
    }

    protected void write(MCRVersionedPath path, byte[] data) throws IOException {
        try (SeekableByteChannel channel = getStorage().newByteChannel(path, new HashSet<>(
            Arrays.asList(StandardOpenOption.CREATE, StandardOpenOption.WRITE)))) {
            channel.write(ByteBuffer.wrap(data));
        }
    }

    protected byte[] read(MCRVersionedPath path) throws IOException {
        try (SeekableByteChannel channel = getStorage().newByteChannel(path, new HashSet<>(
            List.of(StandardOpenOption.READ)))) {
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            return buffer.array();
        }
    }

    /**
     * Eviction strategy based on the number of files.
     *
     * @param maxFiles amount of files
     */
    protected record FileCountEvictionStrategy(int maxFiles) implements MCROCFLEvictionStrategy {

        @Override
        public boolean shouldEvict(long totalFiles, long totalAllocation) {
            return totalFiles > maxFiles;
        }

    }

}
