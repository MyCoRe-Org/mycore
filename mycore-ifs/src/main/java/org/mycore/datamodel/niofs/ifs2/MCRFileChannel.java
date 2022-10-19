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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

import org.mycore.common.content.streams.MCRMD5InputStream;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRFileChannel extends FileChannel {

    private final MCRPath path;

    private FileChannel baseChannel;

    private MCRFile file;

    private boolean write;

    private boolean modified;

    private boolean create;

    /**
     * MyCoRe implementation of a Java NIO FileChannel
     * 
     * @param path - the MyCoRe path object
     * @param file - the MyCoRe file object
     * @param baseChannel - the base channel
     * @param write - true, if the FileChannel is writeable
     * @param create - true, if the FileChannel can create new files 
     * 
     * @see FileChannel
     */
    public MCRFileChannel(MCRPath path, MCRFile file, FileChannel baseChannel, boolean write, boolean create) {
        this.path = path;
        this.file = file;
        this.baseChannel = baseChannel;
        this.write = write;
        this.modified = false;
        this.create = create;
        if (write && !path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute with write operations");
        }
    }

    public void implCloseChannel() throws IOException {
        baseChannel.close(); //MCR-1003 close before updating metadata, as we read attributes from this file later
        updateMetadata();
    }

    private void updateMetadata() throws IOException {
        if (!write || !modified) {
            if (create) {
                MCRPathEventHelper.fireFileCreateEvent(path, file.getBasicFileAttributes());
            }
            return;
        }
        MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        FileChannel md5Channel = (FileChannel) Files.newByteChannel(file.getLocalPath(), StandardOpenOption.READ);
        try {
            long position = 0;
            long size = md5Channel.size();
            while (position < size) {
                long remainingSize = size - position;
                final ByteBuffer byteBuffer = md5Channel.map(MapMode.READ_ONLY, position,
                    Math.min(remainingSize, Integer.MAX_VALUE));
                while (byteBuffer.hasRemaining()) {
                    md5Digest.update(byteBuffer);
                }
                position += byteBuffer.limit();
            }
        } finally {
            if (!md5Channel.equals(baseChannel)) {
                md5Channel.close();
            }
        }
        String md5 = MCRContentInputStream.getMD5String(md5Digest.digest());
        file.setMD5(md5);
        final MCRFileAttributes<String> basicFileAttributes = file.getBasicFileAttributes();
        if (create) {
            MCRPathEventHelper.fireFileCreateEvent(path, basicFileAttributes);
        } else {
            MCRPathEventHelper.fireFileUpdateEvent(path, basicFileAttributes);
        }
    }

    //Delegate to baseChannel

    public int read(ByteBuffer dst) throws IOException {
        return baseChannel.read(dst);
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return baseChannel.read(dsts, offset, length);
    }

    public int write(ByteBuffer src) throws IOException {
        modified = true;
        return baseChannel.write(src);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        modified = true;
        return baseChannel.write(srcs, offset, length);
    }

    public long position() throws IOException {
        return baseChannel.position();
    }

    public FileChannel position(long newPosition) throws IOException {
        return baseChannel.position(newPosition);
    }

    public long size() throws IOException {
        return baseChannel.size();
    }

    public FileChannel truncate(long size) throws IOException {
        modified = true;
        return baseChannel.truncate(size);
    }

    public void force(boolean metaData) throws IOException {
        baseChannel.force(metaData);
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return baseChannel.transferTo(position, count, target);
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        modified = true;
        return baseChannel.transferFrom(src, position, count);
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        return baseChannel.read(dst, position);
    }

    public int write(ByteBuffer src, long position) throws IOException {
        modified = true;
        return baseChannel.write(src, position);
    }

    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        if (write) {
            modified = true;
        }
        return baseChannel.map(mode, position, size);
    }

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return baseChannel.lock(position, size, shared);
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return baseChannel.tryLock(position, size, shared);
    }

}
