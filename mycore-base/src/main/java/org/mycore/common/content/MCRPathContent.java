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

package org.mycore.common.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MCRContent implementation that uses Java 7 {@link FileSystem} features.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRPathContent extends MCRContent implements MCRSeekableChannelContent {

    private static final Logger LOGGER = LogManager.getLogger();

    private Path path;

    private static int BUFFER_SIZE = 8192;

    public MCRPathContent(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.MCRSeekableChannelContent#getSeekableByteChannel()
     */
    @Override
    public SeekableByteChannel getSeekableByteChannel() throws IOException {
        return Files.newByteChannel(path, StandardOpenOption.READ);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path, StandardOpenOption.READ);
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return getSeekableByteChannel();
    }

    @Override
    public byte[] asByteArray() throws IOException {
        long len = length();
        if (len > Integer.MAX_VALUE) {
            throw new IOException("Content does not fit into byte array: " + len);
        }
        int size = (int) len;
        try (SeekableByteChannel channel = getSeekableByteChannel()) {
            if (channel instanceof FileChannel) {
                MappedByteBuffer mappedByteBuffer = ((FileChannel) channel).map(MapMode.READ_ONLY, 0, size);
                if (mappedByteBuffer.hasArray()) {
                    LOGGER.debug(() -> path + " -> shortcut via MappedByteBuffer available");
                    return mappedByteBuffer.array();
                }
                LOGGER.debug(() -> path + " -> shortcut via MappedByteBuffer NOT available");
                byte[] returns = new byte[size];
                int nGet, offset;
                while (mappedByteBuffer.hasRemaining()) {
                    offset = mappedByteBuffer.position();
                    nGet = Math.min(mappedByteBuffer.remaining(), BUFFER_SIZE);
                    mappedByteBuffer.get(returns, offset, nGet);
                }
                return returns;
            }
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size);
            if (!byteBuffer.hasArray()) {
                LOGGER.debug(() -> "Your OS does not support byte[] backed ByteBuffer.");
                byteBuffer.clear();
                byteBuffer = ByteBuffer.wrap(new byte[size]);
            }
            while (byteBuffer.hasRemaining()) {
                if (channel.read(byteBuffer) < 0) {
                    throw new IOException("Bytebuffer size does not match");
                }
            }
            return byteBuffer.array();
        }
    }

    @Override
    public long length() throws IOException {
        return Files.size(path);
    }

    @Override
    public long lastModified() throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    @Override
    public String getETag() throws IOException {
        Object fileKey = Files.getAttribute(path, "fileKey");
        if (fileKey instanceof String) {
            return fileKey.toString();
        }
        return super.getETag();
    }

    @Override
    public String getMimeType() throws IOException {
        return mimeType == null ? Files.probeContentType(path) : mimeType;
    }

    @Override
    public String getSystemId() {
        return path.toUri().toString();
    }

    @Override
    public String getName() {
        return name == null ? path.getFileName().toString() : super.getName();
    }

    @Override
    public void sendTo(OutputStream out) throws IOException {
        Files.copy(path, out);
    }

    @Override
    public void sendTo(File target) throws IOException {
        Files.copy(path, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void sendTo(Path target, CopyOption... options) throws IOException {
        Files.copy(path, target, options);
    }

}
