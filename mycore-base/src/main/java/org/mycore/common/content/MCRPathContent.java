/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.mycore.datamodel.niofs.MCRFileAttributes;

/**
 * MCRContent implementation that uses Java 7 {@link FileSystem} features.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRPathContent extends MCRContent implements MCRSeekableChannelContent {

    private Path path;

    private BasicFileAttributes attrs;

    public MCRPathContent(Path path) {
        this(path, null);
    }

    public MCRPathContent(Path path, BasicFileAttributes attrs) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.attrs = attrs;
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
        return Files.readAllBytes(path);
    }

    @Override
    public long length() throws IOException {
        return attrs != null ? attrs.size() : Files.size(path);
    }

    @Override
    public long lastModified() throws IOException {
        return (attrs != null ? attrs.lastModifiedTime() : Files.getLastModifiedTime(path)).toMillis();
    }

    @Override
    public String getETag() throws IOException {
        if (attrs instanceof MCRFileAttributes<?> fAttrs) {
            return fAttrs.digest().toHexString();
        }

        if (Files.getFileStore(path).supportsFileAttributeView("md5")) {
            Object fileKey = Files.getAttribute(path, "md5:md5");
            if (fileKey instanceof String s) {
                return s;
            }
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
