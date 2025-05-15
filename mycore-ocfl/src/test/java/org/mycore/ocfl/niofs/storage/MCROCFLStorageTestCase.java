package org.mycore.ocfl.niofs.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;
import org.mycore.datamodel.niofs.MCRVersionedPath;

public abstract class MCROCFLStorageTestCase {

    @TempDir
    public Path folder;

    protected Path rootPath;

    protected MCRVersionedPath path1;

    protected MCRVersionedPath path2;

    protected MCRVersionedPath path3;

    public void setUp() throws Exception {
        this.rootPath = folder.resolve("ocfl-rolling-cache");
        path1 = MCRVersionedPath.head("owner1", "file1");
        path2 = MCRVersionedPath.head("owner1", "file2");
        path3 = MCRVersionedPath.head("owner1", "file3");
    }

    protected abstract MCROCFLFileStorage getStorage();

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
