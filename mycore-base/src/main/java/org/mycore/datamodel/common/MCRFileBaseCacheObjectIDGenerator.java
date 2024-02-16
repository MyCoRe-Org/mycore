package org.mycore.datamodel.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MCRFileBaseCacheObjectIDGenerator implements MCRObjectIDGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    static ConcurrentHashMap<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private static Path getCacheFilePath(String baseId) {

        Path dataDir = getDataDirPath();

        Path idCachePath = dataDir.resolve("id_cache");
        if (!Files.exists(idCachePath)) {
            synchronized (MCRFileBaseCacheObjectIDGenerator.class) {
                if (!Files.exists(idCachePath)) {
                    try {
                        Files.createDirectory(idCachePath);
                    } catch (IOException e) {
                        throw new MCRException(
                            "Could not create " + idCachePath.toAbsolutePath() + " directory", e);
                    }
                }
            }
        }

        Path cacheFile = MCRUtils.safeResolve(idCachePath, baseId);
        if (!Files.exists(cacheFile)) {
            synchronized (MCRFileBaseCacheObjectIDGenerator.class) {
                if (!Files.exists(cacheFile)) {
                    try {
                        Files.createFile(cacheFile);
                    } catch (IOException e) {
                        throw new MCRException("Could not create " + cacheFile.toAbsolutePath(), e);
                    }
                }
            }
        }
        return cacheFile;
    }

    static Path getDataDirPath() {
        Path path = Paths.get(MCRConfiguration2.getStringOrThrow("MCR.datadir"));
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new MCRException("Data directory does not exist or is not a directory: " + path);
        }
        return path;
    }

    private static void writeNewID(MCRObjectID nextID, ByteBuffer buffer, FileChannel channel, Path cacheFile)
        throws IOException {
        buffer.clear();
        channel.position(0);
        buffer.putInt(nextID.getNumberAsInteger());
        buffer.flip();
        int written = channel.write(buffer);
        if (written != Integer.BYTES) {
            throw new MCRException("Could not write new ID to " + cacheFile.toAbsolutePath());
        }
    }

    public void setNextFreeId(String baseId, int next) {
        Path cacheFile = getCacheFilePath(baseId);

        try (
            FileChannel channel = FileChannel.open(cacheFile, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC, StandardOpenOption.CREATE);){
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            channel.position(0);
            writeNewID(MCRObjectID.getInstance(MCRObjectID.formatID(baseId, next-1)), buffer, channel, cacheFile);
        } catch (FileNotFoundException e) {
            throw new MCRException("Could not create " + cacheFile.toAbsolutePath(), e);
        } catch (IOException e) {
            throw new MCRException("Could not open " + cacheFile.toAbsolutePath(), e);
        }
    }

    @Override
    public MCRObjectID getNextFreeId(String baseId, int maxInWorkflow) {
        Path cacheFile = getCacheFilePath(baseId);

        MCRObjectID nextID;

        ReentrantReadWriteLock lock = locks.computeIfAbsent(baseId, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        try {
            writeLock.lock();
            try (
                FileChannel channel = FileChannel.open(cacheFile, StandardOpenOption.READ, StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC);
                FileLock fileLock = channel.lock()) {

                ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
                buffer.clear();
                channel.position(0);
                int bytesRead = channel.read(buffer);

                if (bytesRead <= 0) {
                    LOGGER.info("No ID found in " + cacheFile.toAbsolutePath());
                    // empty file -> new currentID is 1
                    nextID = MCRObjectID.getInstance(MCRObjectID.formatID(baseId, maxInWorkflow + 1));
                    writeNewID(nextID, buffer, channel, cacheFile);
                } else if (bytesRead == Integer.BYTES) {
                    buffer.flip();
                    int lastID = buffer.getInt();
                    nextID = MCRObjectID.getInstance(MCRObjectID.formatID(baseId, lastID + maxInWorkflow + 1));
                    writeNewID(nextID, buffer, channel, cacheFile);
                } else {
                    throw new MCRException("Content is not Int Number " + cacheFile.toAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                throw new MCRException("Could not create " + cacheFile.toAbsolutePath(), e);
            } catch (IOException e) {
                throw new MCRException("Could not open " + cacheFile.toAbsolutePath(), e);
            }
        } finally {
            writeLock.unlock();
        }

        return nextID;
    }

    @Override
    public MCRObjectID getLastID(String baseId) {
        Path cacheFilePath = getCacheFilePath(baseId);
        ReentrantReadWriteLock lock = locks.computeIfAbsent(baseId, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();

            try (FileChannel channel = FileChannel.open(cacheFilePath)) {
                ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
                buffer.clear();
                channel.position(0);
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {
                    return null;
                } else if (bytesRead == Integer.BYTES) {
                    buffer.flip();
                    int lastID = buffer.getInt();
                    return MCRObjectID.getInstance(MCRObjectID.formatID(baseId, lastID));
                } else {
                    throw new MCRException("Content is not Int Number " + cacheFilePath.toAbsolutePath());
                }
            } catch (IOException e) {
                throw new MCRException("Could not open " + cacheFilePath.toAbsolutePath(), e);
            }
        } finally {
            readLock.unlock();
        }
    }

}
