package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRFileAttributes;

abstract class MCRBasicFileAttributeViewImpl implements BasicFileAttributeView {
    private static Logger LOGGER = LogManager.getLogger(MCRBasicFileAttributeViewImpl.class);

    public MCRBasicFileAttributeViewImpl() {
        super();
    }

    static MCRFileAttributes<String> readAttributes(MCRFilesystemNode node) throws IOException {
        if (node instanceof MCRFile) {
            MCRFile file = (MCRFile) node;
            Path localFilePath = file.getLocalFile().toPath();
            BasicFileAttributes localFileAttributes = Files.readAttributes(localFilePath, BasicFileAttributes.class);
            FileTime creationTime = localFileAttributes.creationTime(); //unavailable in IFS1
            FileTime lastModified = FileTime.fromMillis(file.getLastModified().getTimeInMillis());
            if (lastModified.compareTo(creationTime) < 0) {
                LOGGER.debug("lastModified time is before creation time: " + node.toPath().toString());
            }
            FileTime lastAccessTime = localFileAttributes.lastAccessTime(); //unavailable in IFS1
            if (localFileAttributes.size() != file.getSize()) {
                LOGGER.error(MessageFormat.format(
                    "File size mismatch detected for {0}. Local file should be {1} bytes long but is {2} bytes long.",
                    node.getPath(),
                    file.getSize(), localFileAttributes.size()));
            }
            return MCRFileAttributes.file(file.getID(), file.getSize(), file.getMD5(), creationTime, lastModified,
                lastAccessTime);
        }
        return MCRFileAttributes.directory(node.getID(), node.getSize(),
            FileTime.fromMillis(node.getLastModified().getTimeInMillis()));
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public MCRFileAttributes<String> readAttributes() throws IOException {
        MCRFilesystemNode node = resolveNode();
        return readAttributes(node);
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        MCRFilesystemNode node = resolveNode();
        if (node instanceof MCRFile) {
            MCRFile file = (MCRFile) node;
            file.adjustMetadata(lastModifiedTime, file.getMD5(), file.getSize());
            Files.getFileAttributeView(file.getLocalFile().toPath(), BasicFileAttributeView.class).setTimes(
                lastModifiedTime,
                lastAccessTime, createTime);
        } else if (node instanceof MCRDirectory) {
            LOGGER.warn("Setting times on directories is not supported: " + node.toPath());
        }
    }

    protected abstract MCRFilesystemNode resolveNode() throws IOException;

}
