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

package org.mycore.ocfl.niofs.channels;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

import io.ocfl.api.exception.CorruptObjectException;
import io.ocfl.api.model.OcflObjectVersionFile;
import io.ocfl.api.model.SizeDigestAlgorithm;

/**
 * The {@code MCROCFLReadableByteChannel} class implements the {@link SeekableByteChannel} interface
 * to provide read-only access to a file within an OCFL (Oxford Common File Layout) object version.
 *
 * <p>This channel does not support write or truncate operations and should only be used for
 * reading purposes.
 *
 * <p>Unsupported operations:
 * <ul>
 *     <li>{@link #write(ByteBuffer)}: Throws {@link UnsupportedOperationException} as writing is not supported.</li>
 *     <li>{@link #truncate(long)}: Throws {@link UnsupportedOperationException} as truncation is not supported.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 *     OcflObjectVersionFile ocflFile = ...; // Initialize OCFL file object
 *     try (MCROCFLReadableByteChannel channel = new MCROCFLReadableByteChannel(ocflFile)) {
 *         ByteBuffer buffer = ByteBuffer.allocate(1024);
 *         int bytesRead = channel.read(buffer);
 *         // Process buffer...
 *     } catch (IOException e) {
 *         // Handle exception...
 *     }
 * </pre>
 *
 * @see SeekableByteChannel
 * @see OcflObjectVersionFile
 */
public class MCROCFLReadableByteChannel implements SeekableByteChannel {

    protected long position;

    protected boolean isOpen;

    protected OcflObjectVersionFile file;

    /**
     * Constructs a new {@code MCROCFLReadableByteChannel} for reading from the specified OCFL object version file.
     *
     * @param file the {@link OcflObjectVersionFile} from which to read data
     */
    public MCROCFLReadableByteChannel(OcflObjectVersionFile file) {
        this.file = file;
        this.position = 0;
        this.isOpen = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if (!this.isOpen) {
            throw new ClosedChannelException();
        }
        // Get the number of bytes remaining in the buffer
        int remaining = byteBuffer.remaining();
        byte[] buffer = new byte[remaining];
        // Get the data from the current position with a range that fits the buffer's remaining space
        InputStream inputStream = this.file.getRange(this.position, this.position + remaining - 1);
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1) {
            return -1;
        }
        // Write the bytes read from input stream to the byteBuffer
        byteBuffer.put(buffer, 0, bytesRead);
        // Update the current position
        this.position += bytesRead;
        return bytesRead;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException("write not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long position() throws IOException {
        if (!this.isOpen) {
            throw new ClosedChannelException();
        }
        return this.position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel position(long position) throws IOException {
        if (!this.isOpen) {
            throw new ClosedChannelException();
        }
        if (position < 0) {
            throw new IllegalArgumentException("position cannot be negative");
        }
        this.position = position;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() throws IOException {
        if (!this.isOpen) {
            throw new ClosedChannelException();
        }
        String sizeAsString = this.file.getFixity().get(new SizeDigestAlgorithm());
        if (sizeAsString == null) {
            throw new CorruptObjectException("Missing size digest fixity for '" + file.getStorageRelativePath() + "'");
        }
        return Long.parseLong(sizeAsString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel truncate(long l) throws IOException {
        throw new UnsupportedOperationException("truncate not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return this.isOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        isOpen = false;
    }

}
