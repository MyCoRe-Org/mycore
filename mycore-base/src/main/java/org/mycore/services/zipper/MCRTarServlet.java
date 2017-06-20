package org.mycore.services.zipper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import javax.servlet.ServletOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Uses TAR format to deliver requested content.
 * 
 * This servlet produces TAR files as defined in POSIX.1-2001 standard and UTF-8 encoding for file names.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRTarServlet extends MCRCompressServlet<TarArchiveOutputStream> {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRTarServlet.class);

    @Override
    protected void sendCompressedDirectory(MCRPath file, BasicFileAttributes attrs,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getFilename(file), TarArchiveEntry.LF_DIR);
        entry.setModTime(attrs.lastModifiedTime().toMillis());
        container.putArchiveEntry(entry);
        container.closeArchiveEntry();
    }

    @Override
    protected void sendCompressedFile(MCRPath file, BasicFileAttributes attrs,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(getFilename(file));
        entry.setModTime(attrs.lastModifiedTime().toMillis());
        entry.setSize(attrs.size());
        container.putArchiveEntry(entry);
        try {
            Files.copy(file, container);
        } finally {
            container.closeArchiveEntry();
        }
    }

    @Override
    protected void sendMetadataCompressed(String fileName, byte[] content, long lastModified,
        TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(fileName);
        entry.setModTime(lastModified);
        entry.setSize(content.length);
        container.putArchiveEntry(entry);
        container.write(content);
        container.closeArchiveEntry();
    }

    @Override
    protected String getMimeType() {
        return "application/x-tar";
    }

    @Override
    protected String getFileExtension() {
        return "tar";
    }

    @Override
    protected TarArchiveOutputStream createContainer(ServletOutputStream sout, String comment) {
        LOGGER.info("Constructing tar archive: " + comment);
        TarArchiveOutputStream tout = new TarArchiveOutputStream(sout, "UTF8");
        tout.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tout;
    }

    @Override
    protected void disposeContainer(TarArchiveOutputStream container) throws IOException {
        container.finish();
    }
}
