/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class generates object ids based on a file based cache. The cache is used to store the last generated id for a
 * given base id. The cache file is located in the data directory of MyCoRe and is named "id_cache" and contains one
 * file for each base id. The file contains the last generated id as a string.
 */
public class MCRFileBaseCacheObjectIDGenerator implements MCRObjectIDGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    static ConcurrentHashMap<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private static Path getCacheFilePath(String baseId) {
        Path idCachePath = getCacheDirPath();

        Path cacheFile = MCRUtils.safeResolve(idCachePath, baseId);
        if (Files.exists(cacheFile)) {
            return cacheFile;
        }

        synchronized (MCRFileBaseCacheObjectIDGenerator.class) {
            if (!Files.exists(cacheFile)) {
                try {
                   return Files.createFile(cacheFile);
                } catch (IOException e) {
                    throw new MCRException("Could not create " + cacheFile.toAbsolutePath(), e);
                }
            }
        }

        return cacheFile;
    }

    private static Path getCacheDirPath() {
        Path dataDir = getDataDirPath();

        Path idCachePath = dataDir.resolve("id_cache");
        if (Files.exists(idCachePath)) {
            return idCachePath;
        }
        synchronized (MCRFileBaseCacheObjectIDGenerator.class) {
            if (!Files.exists(idCachePath)) {
                try {
                    Files.createDirectory(idCachePath);
                } catch (IOException e) {
                    throw new MCRException("Could not create " + idCachePath.toAbsolutePath() + " directory", e);
                }
            }
        }
        return idCachePath;
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
        byte[] idAsBytes = nextID.toString().getBytes(StandardCharsets.UTF_8);
        buffer.put(idAsBytes);
        buffer.flip();
        int written = channel.write(buffer);
        if (written != idAsBytes.length) {
            throw new MCRException("Could not write new ID to " + cacheFile.toAbsolutePath());
        }
    }

    /**
     * Set the next free id for the given baseId. Should only be used for migration purposes and the caller has to make
     * sure that the cache file is not used by another process.
     * @param baseId the base id
     * @param next the next free id to be returned by getNextFreeId
     */
    public void setNextFreeId(String baseId, int next) {
        Path cacheFile = getCacheFilePath(baseId);

        int idLengthInBytes = MCRObjectID.formatID(baseId, 1).getBytes(StandardCharsets.UTF_8).length;
        try (
            FileChannel channel = FileChannel.open(cacheFile, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC, StandardOpenOption.CREATE);){
            ByteBuffer buffer = ByteBuffer.allocate(idLengthInBytes);
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

                int idLengthInBytes = MCRObjectID.formatID(baseId, 1).getBytes(StandardCharsets.UTF_8).length;
                ByteBuffer buffer = ByteBuffer.allocate(idLengthInBytes);
                buffer.clear();
                channel.position(0);
                int bytesRead = channel.read(buffer);
                if (bytesRead <= 0) {
                    LOGGER.info("No ID found in " + cacheFile.toAbsolutePath());
                    // empty file -> new currentID is 1
                    nextID = MCRObjectID.getInstance(MCRObjectID.formatID(baseId, maxInWorkflow + 1));
                    writeNewID(nextID, buffer, channel, cacheFile);
                } else if (bytesRead == idLengthInBytes) {
                    buffer.flip();
                    MCRObjectID objectID = readObjectIDFromBuffer(idLengthInBytes, buffer);
                    int lastID = objectID.getNumberAsInteger();
                    nextID = MCRObjectID.getInstance(MCRObjectID.formatID(baseId, lastID + maxInWorkflow + 1));
                    writeNewID(nextID, buffer, channel, cacheFile);
                } else {
                    throw new MCRException("Content has different id length " + cacheFile.toAbsolutePath());
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

    private static MCRObjectID readObjectIDFromBuffer(int idLengthBytes, ByteBuffer buffer) {
        byte[] idBytes = new byte[idLengthBytes];
        buffer.get(idBytes);
        String lastIDString = new String(idBytes, StandardCharsets.UTF_8);
        return MCRObjectID.getInstance(lastIDString);
    }

    @Override
    public MCRObjectID getLastID(String baseId) {
        Path cacheFilePath = getCacheFilePath(baseId);
        ReentrantReadWriteLock lock = locks.computeIfAbsent(baseId, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            int idLengthInBytes = MCRObjectID.formatID(baseId, 1).getBytes(StandardCharsets.UTF_8).length;

            try (FileChannel channel = FileChannel.open(cacheFilePath)) {
                ByteBuffer buffer = ByteBuffer.allocate(idLengthInBytes);
                buffer.clear();
                channel.position(0);
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {
                    // empty file -> no ID found
                    return null;
                } else if (bytesRead == idLengthInBytes) {
                    buffer.flip();
                    return readObjectIDFromBuffer(idLengthInBytes, buffer);
                } else {
                    throw new MCRException("Content has different id length " + cacheFilePath.toAbsolutePath());
                }
            } catch (IOException e) {
                throw new MCRException("Could not open " + cacheFilePath.toAbsolutePath(), e);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<String> getBaseIDs() {
        Path cacheDir = getCacheDirPath();

        try (Stream<Path> list = Files.list(cacheDir);) {
            List<String> baseIdList = list.filter(Files::isRegularFile)
                    .map(Path::getFileName).map(Path::toString)
                    .collect(Collectors.toList());
            return baseIdList;
        } catch (IOException e) {
            throw new MCRException("Could not detect cache files!", e);
        }

    }

}
