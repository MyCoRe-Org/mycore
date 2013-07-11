package org.mycore.services.zipper;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Uses TAR format to deliver requested content.
 * 
 * This servlet produces TAR files as defined in POSIX.1-2001 standard and UTF-8 encoding for file names.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRTarServlet extends MCRCompressServlet<TarArchiveOutputStream> {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRTarServlet.class);

    @Override
    protected void sendCompressed(MCRFile file, TarArchiveOutputStream container) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file.getPath());
        entry.setModTime(file.getLastModified().getTimeInMillis());
        entry.setSize(file.getSize());
        container.putArchiveEntry(entry);
        try (InputStream in = file.getContentAsInputStream()) {
            IOUtils.copy(in, container);
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
