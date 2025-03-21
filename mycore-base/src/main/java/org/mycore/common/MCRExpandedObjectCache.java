package org.mycore.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public final class MCRExpandedObjectCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CACHE_ROOT_PATH_PROPERTY = "MCR.ObjectExpander.Cache.Path";
    private static volatile MCRExpandedObjectCache instance;
    private Map<MCRObjectID, ReentrantReadWriteLock> idLocks = new ConcurrentHashMap<>();

    private MCRExpandedObjectCache() {
        // Private constructor to prevent instantiation
    }

    public static MCRExpandedObjectCache getInstance() {
        if (instance == null) {
            synchronized (MCRExpandedObjectCache.class) {
                if (instance == null) {
                    instance = new MCRExpandedObjectCache();
                }
            }
        }
        return instance;
    }

    // false negative: https://github.com/pmd/pmd/issues/4516
    @SuppressWarnings("PMD.UnusedLocalVariable")
    private static MCRExpandedObject readExpandedObject(Path expandedObjectPath) {
        try (FileChannel channel =
            FileChannel.open(expandedObjectPath, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC);
            FileLock fl = channel.lock()) {
            byte[] fileContent = new byte[(int) channel.size()];
            ByteBuffer buffer = ByteBuffer.wrap(fileContent);
            channel.read(buffer);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document jdom = saxBuilder.build(new ByteArrayInputStream(fileContent));
            return new MCRExpandedObject(jdom);
        } catch (IOException | JDOMException e) {
            throw new MCRException(e);
        }
    }

    public MCRExpandedObject getExpandedObject(MCRObjectID id, Supplier<MCRExpandedObject> expandedObjectSupplier) {
        ReentrantReadWriteLock lock = idLocks.computeIfAbsent(id, idx -> new ReentrantReadWriteLock());
        Path expandedObjectPath = getExpandedObjectPath(id);

        try {
            lock.readLock().lock();
            if (Files.exists(expandedObjectPath)) {
                return readExpandedObject(expandedObjectPath);
            }
        } finally {
            lock.readLock().unlock();
        }
        try {
            lock.writeLock().lock();
            if (Files.exists(expandedObjectPath)) {
                return readExpandedObject(expandedObjectPath);
            }
            createParentPaths(expandedObjectPath);
            MCRExpandedObject expandedObject = expandedObjectSupplier.get();
            writeExpandedObject(expandedObject, expandedObjectPath);
            return expandedObject;
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void clear(MCRObjectID id) {
        LOGGER.info("Clearing expanded object {}", id);
        ReentrantReadWriteLock lock = idLocks.computeIfAbsent(id, idx -> new ReentrantReadWriteLock());
        Path expandedObjectPath = getExpandedObjectPath(id);
        try {
            lock.writeLock().lock();
            if (Files.exists(expandedObjectPath)) {
                Files.delete(expandedObjectPath);
            }
        } catch (IOException e) {
            throw new MCRException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // false negative: https://github.com/pmd/pmd/issues/4516
    @SuppressWarnings("PMD.UnusedLocalVariable")
    private void writeExpandedObject(MCRExpandedObject expandedObject, Path expandedObjectPath) {
        LOGGER.info("Writing expanded object {} to cache.", expandedObject);
        try (FileChannel channel =
            FileChannel.open(expandedObjectPath, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            FileLock fl = channel.lock()) {
            XMLOutputter xmlOutput = new XMLOutputter();
            String file = xmlOutput.outputString(expandedObject.createXML());
            byte[] fileContent = file.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(fileContent);
            int written = channel.write(buffer);
            if (written != fileContent.length) {
                throw new MCRException("Could not write all bytes to file: " + expandedObjectPath);
            }
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    private void createParentPaths(Path path) {
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException e) {
            throw new MCRException("Error creating directories for path: " + path, e);
        }
    }

    private Path getExpandedObjectPath(MCRObjectID id) {
        Objects.requireNonNull(id, "ID must not be null");
        String numberAsString = id.getNumberAsString();
        int folderSize = 3;
        int folders = (int) Math.ceil(numberAsString.length() / (double) folderSize);
        Path result = getCacheRootPath().resolve(id.getProjectId()).resolve(id.getTypeId());
        for (int i = 0; i < folders; i++) {
            boolean last = (i == folders - 1);
            String fileOrFolderName =
                last ? id + ".xml"
                    : numberAsString.substring(folderSize * i, Math.min(folderSize * i + 3, numberAsString.length()));
            result = result.resolve(fileOrFolderName);
        }
        return result;
    }

    /**
     * @return the root path for the expanded object cache
     */
    public Path getCacheRootPath() {
        return MCRConfiguration2.getOrThrow(CACHE_ROOT_PATH_PROPERTY, Paths::get);
    }

}
