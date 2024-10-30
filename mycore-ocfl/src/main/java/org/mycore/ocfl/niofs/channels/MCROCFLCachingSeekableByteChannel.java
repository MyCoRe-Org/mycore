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

package org.mycore.ocfl.niofs.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code MCROCFLCachingSeekableByteChannel} class implements a {@link SeekableByteChannel} that
 * delegates read operations to another {@link SeekableByteChannel} and simultaneously stores the
 * read data into a file at the specified {@link java.nio.file.Path}.
 *
 * <p>This class also provides a method to check if the entire file has been read and
 * is fully available at the provided path.
 *
 * <p>Be aware that reading from the end of the delegated channel will result in a cache file of the same size.
 * E.g. reading the last 10 Bytes of a 10 GB file will result in 10 GB cache file.
 */
public class MCROCFLCachingSeekableByteChannel implements SeekableByteChannel {

    private final SeekableByteChannel delegate;
    private final Path cacheFilePath;
    private final FileChannel cacheFileChannel; // Use FileChannel for writing
    private final List<Range> cachedRanges; // Store ranges of cached data
    private final long delegateSize;
    private boolean isOpen;

    /**
     * Constructs a new {@code CachingSeekableByteChannel} with the given delegate channel and path.
     * Each time data is read, it is also written to the file at the provided path.
     *
     * @param delegate the underlying {@link SeekableByteChannel} to delegate read operations
     * @param cacheFilePath the {@link Path} where the read data will be stored
     * @throws IOException if an I/O error occurs during initialization
     */
    public MCROCFLCachingSeekableByteChannel(SeekableByteChannel delegate, Path cacheFilePath) throws IOException {
        this.delegate = delegate;
        this.cacheFilePath = cacheFilePath;
        this.cacheFileChannel = FileChannel.open(cacheFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        this.cachedRanges = new ArrayList<>();
        this.delegateSize = delegate.size();
        this.isOpen = true;
    }

    /**
     * Reads a sequence of bytes from the delegate channel into the given byte buffer and stores the
     * read data into the file at {@link Path}.
     *
     * <p>The method also tracks which byte ranges have been cached to ensure that
     * the cached file matches the original file.
     *
     * @param byteBuffer the buffer into which bytes are transferred
     * @return the number of bytes read, possibly zero, or {@code -1} if the channel has reached end-of-file
     * @throws IOException if an I/O error occurs, or if the channel is closed
     */
    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if (!isOpen) {
            throw new IOException("Channel is closed");
        }

        // Delegate the reading operation to the underlying channel
        long currentPosition = delegate.position(); // Track current position
        int bytesRead = delegate.read(byteBuffer);

        if (bytesRead > 0) {
            // Write the read bytes into the file using FileChannel
            byteBuffer.flip();
            cacheFileChannel.position(currentPosition);
            cacheFileChannel.write(byteBuffer);

            // Track the range that was cached
            addCachedRange(currentPosition, currentPosition + bytesRead - 1);

            // Prepare the buffer for further operations
            byteBuffer.clear();
        }

        return bytesRead;
    }

    /**
     * Adds a cached range and merges it with existing ranges if necessary.
     *
     * @param start the start position of the cached range
     * @param end the end position of the cached range
     */
    private void addCachedRange(long start, long end) {
        Range newRange = new Range(start, end);

        // Try to merge with existing ranges
        for (Range existingRange : cachedRanges) {
            if (existingRange.overlapsOrAdjacent(newRange)) {
                existingRange.merge(newRange);
                return;
            }
        }

        // If no merge was possible, add as a new range
        cachedRanges.add(newRange);
    }

    /**
     * Returns {@code true} if the entire file has been read and the cached file matches the original file.
     *
     * @return {@code true} if the cache file is complete, {@code false} otherwise
     * @throws IOException if an I/O error occurs while checking file sizes
     */
    public boolean isFileComplete() throws IOException {
        // Ensure the sizes are the same
        if (Files.size(this.cacheFilePath) != this.delegateSize) {
            return false;
        }

        // Check if the cached ranges cover the entire file
        if (this.cachedRanges.isEmpty()) {
            return false;
        }

        // Ensure we have a single range that covers the entire file
        Range firstRange = this.cachedRanges.getFirst();
        return firstRange.start == 0 && firstRange.end == this.delegateSize - 1;
    }

    /**
     * Unsupported operation. Writing is not allowed in this channel.
     *
     * @param byteBuffer the buffer from which bytes are to be transferred
     * @return nothing, as this operation is unsupported
     * @throws UnsupportedOperationException always
     */
    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException("write not supported");
    }

    /**
     * Returns the current position of this channel.
     *
     * @return the current position, measured in bytes from the beginning of the file
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long position() throws IOException {
        return this.delegate.position();
    }

    /**
     * Sets the channel's position. The next read will start at this position.
     *
     * @param newPosition the new position, measured in bytes from the beginning of the file
     * @return this channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return this.delegate.position(newPosition);
    }

    /**
     * Returns the size of the file, measured in bytes.
     *
     * @return the size of the file
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long size() throws IOException {
        return this.delegateSize;
    }

    /**
     * Unsupported operation. Truncation is not allowed in this channel.
     *
     * @param size the size to truncate to
     * @return nothing, as this operation is unsupported
     * @throws UnsupportedOperationException always
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("truncate not supported");
    }

    /**
     * Checks if the channel is open.
     *
     * @return {@code true} if the channel is open, {@code false} otherwise
     */
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Closes this channel and releases any resources associated with it.
     * Also closes the cache file output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (isOpen) {
            delegate.close();
            cacheFileChannel.close();
            isOpen = false;
        }
    }

    /**
     * Inner class to represent a range of cached bytes.
     */
    private static class Range {
        long start;
        long end;

        Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        boolean overlapsOrAdjacent(Range other) {
            return this.end + 1 >= other.start && this.start <= other.end + 1;
        }

        void merge(Range other) {
            this.start = Math.min(this.start, other.start);
            this.end = Math.max(this.end, other.end);
        }
    }

}
