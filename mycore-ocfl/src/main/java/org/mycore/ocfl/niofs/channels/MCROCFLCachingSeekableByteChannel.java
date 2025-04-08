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
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The {@code MCROCFLCachingSeekableByteChannel} class implements a {@link SeekableByteChannel} that
 * delegates read operations to another {@link SeekableByteChannel} while caching the read data to a file.
 * <p>
 * This class maintains a cache of byte ranges using a {@link java.util.TreeMap} for efficient merging and lookups.
 * The caching mechanism is thread-safe through synchronization on an internal lock.
 * Note that while this class protects its own caching state, the underlying delegate channel
 * may not be thread safe. External synchronization may be required if it is shared among threads.
 * <p>
 * <b>Usage:</b> Each time data is read, it is written to the cache file at the provided {@code Path}
 * and recorded in the cache range map. The {@code isFileComplete} method verifies whether the entire file has
 * been cached.
 *
 * <p>Be aware that reading from the end of the delegated channel will result in a cache file of the same size.
 * E.g. reading the last 10 Bytes of a 10 GB file will result in 10 GB cache file.
 */
public class MCROCFLCachingSeekableByteChannel implements SeekableByteChannel {

    private final SeekableByteChannel delegate;

    private final Path cacheFilePath;

    private final FileChannel cacheFileChannel;

    private final NavigableMap<Long, Range> cachedRanges;

    private final long delegateSize;

    private boolean isOpen;

    private final Object lock = new Object();

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
        this.cachedRanges = new TreeMap<>();
        this.delegateSize = delegate.size();
        this.isOpen = true;
    }

    /**
     * Reads bytes from the delegate channel into the given byte buffer and caches the data to the file.
     * <p>
     * The method writes the read bytes into the cache file and records the cached byte range in a thread-safe way.
     *
     * @param byteBuffer the buffer into which bytes are transferred
     * @return the number of bytes read, possibly zero, or {@code -1} if the channel has reached end-of-file
     * @throws IOException if an I/O error occurs or if the channel is closed
     */
    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if (!isOpen) {
            throw new IOException("Channel is closed");
        }

        // Record the current position in the delegate channel.
        long currentPosition = delegate.position();
        int bytesRead = delegate.read(byteBuffer);

        if (bytesRead > 0) {
            // Prepare for reading from the buffer.
            byteBuffer.flip();

            // Write the data into the cache file.
            cacheFileChannel.position(currentPosition);
            cacheFileChannel.write(byteBuffer);

            // Add the new cached byte range in a thread-safe way.
            addCachedRange(currentPosition, currentPosition + bytesRead - 1);

            // Clear the buffer for further operations.
            byteBuffer.clear();
        }

        return bytesRead;
    }

    /**
     * Adds a cached byte range and merges it with any overlapping or adjacent ranges.
     * <p>
     * This method uses a {@link TreeMap} to quickly locate overlapping ranges and merges them in a
     * thread-safe block.
     *
     * @param start the start position of the cached range
     * @param end the end position of the cached range
     */
    private void addCachedRange(long start, long end) {
        synchronized (lock) {
            Range newRange = new Range(start, end);

            // Look for a range with a start lower than or equal to the new range.
            var lowerEntry = cachedRanges.floorEntry(newRange.start);
            if (lowerEntry != null && lowerEntry.getValue().overlapsOrAdjacent(newRange)) {
                newRange.merge(lowerEntry.getValue());
                cachedRanges.remove(lowerEntry.getKey());
            }

            // Merge any subsequent ranges that overlap or are adjacent.
            while (true) {
                var higherEntry = cachedRanges.ceilingEntry(newRange.start);
                if (higherEntry == null || !higherEntry.getValue().overlapsOrAdjacent(newRange)) {
                    break;
                }
                newRange.merge(higherEntry.getValue());
                cachedRanges.remove(higherEntry.getKey());
            }

            // Put the newly merged range back into the map.
            cachedRanges.put(newRange.start, newRange);
        }
    }

    boolean hasSingleMergedRange(long expectedStart, long expectedEnd) {
        synchronized (lock) {
            if (cachedRanges.size() != 1) {
                return false;
            }
            Range range = cachedRanges.firstEntry().getValue();
            return range.start == expectedStart && range.end == expectedEnd;
        }
    }

    /**
     * Returns {@code true} if the entire file has been read and cached.
     * <p>
     * This method first compares the size of the cache file with the delegate size,
     * then checks if the cached ranges cover the entire file.
     *
     * @return {@code true} if the cache file is complete, {@code false} otherwise
     * @throws IOException if an I/O error occurs while checking file sizes
     */
    public boolean isFileComplete() throws IOException {
        // Compare the physical cache file size with the delegate's size.
        if (Files.size(this.cacheFilePath) != this.delegateSize) {
            return false;
        }

        // Check if there is a single cached range covering the full file in a thread-safe manner.
        synchronized (lock) {
            if (cachedRanges.size() != 1) {
                return false;
            }
            Range completeRange = cachedRanges.firstEntry().getValue();
            return completeRange.start == 0 && completeRange.end == this.delegateSize - 1;
        }
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
        this.delegate.position(newPosition);
        return this;
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
