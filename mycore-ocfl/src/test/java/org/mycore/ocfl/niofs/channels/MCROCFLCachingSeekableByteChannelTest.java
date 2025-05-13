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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MCROCFLCachingSeekableByteChannelTest {

    @TempDir
    Path tempFolder;

    @Test
    public void testCachingSeekableByteChannel() throws IOException {
        // Create a temporary file with some test data
        Path originalFilePath = tempFolder.resolve("original.txt");
        String testData = "This is some test data for the SeekableByteChannel.";
        Files.write(originalFilePath, testData.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        // Create a temporary file for caching the read data
        Path cacheFilePath = tempFolder.resolve("cached.txt");

        // Create a SeekableByteChannel for the original file
        try (SeekableByteChannel originalChannel = Files.newByteChannel(originalFilePath, StandardOpenOption.READ);
            MCROCFLCachingSeekableByteChannel cachingChannel =
                new MCROCFLCachingSeekableByteChannel(originalChannel, cacheFilePath)) {

            ByteBuffer buffer = ByteBuffer.allocate(10); // Read 10 bytes at a time

            // Read the first 10 bytes
            int bytesRead = cachingChannel.read(buffer);
            assertEquals(10, bytesRead, "Expected to read 10 bytes, but read " + bytesRead + " bytes.");
            buffer.clear();
            assertFalse(cachingChannel.isFileComplete(),
                "File should not be marked complete after reading only 10 bytes.");

            // Jump to position 20 and read another 10 bytes
            cachingChannel.position(20);
            bytesRead = cachingChannel.read(buffer);
            assertEquals(10, bytesRead,
                "Expected to read 10 bytes at position 20, but read " + bytesRead + " bytes.");
            buffer.clear();
            assertFalse(cachingChannel.isFileComplete(),
                "File should not be marked complete after reading two chunks.");

            // Read the remaining bytes sequentially
            cachingChannel.position(10); // Jump back to position 10
            bytesRead = cachingChannel.read(buffer);
            assertEquals(10, bytesRead,
                "Expected to read 10 bytes at position 10, but read " + bytesRead + " bytes.");
            assertFalse(cachingChannel.isFileComplete(),
                "File should not be marked complete after reading three chunks.");

            // Read until the end
            do {
                buffer.clear();
            } while (cachingChannel.read(buffer) != -1);

            // Check if the cached file is complete
            assertTrue(cachingChannel.isFileComplete(),
                "File should be marked complete after reading all the data.");

            // Verify the content of the cached file
            String cachedData = new String(Files.readAllBytes(cacheFilePath));
            assertEquals(testData, cachedData, "Cached file content does not match the original content.");
        }
    }

    @Test
    public void testOverlappingRangesMerging() throws IOException {
        String testData = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Path originalFilePath = tempFolder.resolve("original2.txt");
        Files.write(originalFilePath, testData.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        Path cacheFilePath = tempFolder.resolve("cached2.txt");

        try (SeekableByteChannel originalChannel = Files.newByteChannel(originalFilePath, StandardOpenOption.READ);
            MCROCFLCachingSeekableByteChannel cachingChannel =
                new MCROCFLCachingSeekableByteChannel(originalChannel, cacheFilePath)) {

            // Read 30 bytes from the beginning (positions 0 - 29)
            ByteBuffer buffer = ByteBuffer.allocate(30);
            cachingChannel.position(0);
            int bytesRead = cachingChannel.read(buffer);
            assertEquals(30, bytesRead, "Expected to read 30 bytes, but read " + bytesRead + " bytes.");
            buffer.clear();

            // Read 20 bytes from position 20 (overlap: positions 20 - 39)
            ByteBuffer buffer2 = ByteBuffer.allocate(20);
            cachingChannel.position(20);
            bytesRead = cachingChannel.read(buffer2);
            assertEquals(20, bytesRead,
                "Expected to read 20 bytes at position 20, but read " + bytesRead + " bytes.");

            // Verify that the internal cache has a single merged range [0, 39]
            assertTrue(cachingChannel.hasSingleMergedRange(0, 39),
                "Expected a single merged cached range from 0 to 39.");
        }
    }

    @Test
    public void testReadDoesNotExceedDelegateSize() throws IOException {
        String testData = "1234567890";
        Path originalFilePath = tempFolder.resolve("tiny.txt");
        Files.write(originalFilePath, testData.getBytes());

        Path cacheFilePath = tempFolder.resolve("cached_tiny.txt");

        try (SeekableByteChannel originalChannel = Files.newByteChannel(originalFilePath, StandardOpenOption.READ);
            MCROCFLCachingSeekableByteChannel cachingChannel =
                new MCROCFLCachingSeekableByteChannel(originalChannel, cacheFilePath)) {

            // Seek near end
            cachingChannel.position(9); // only one byte left
            ByteBuffer buffer = ByteBuffer.allocate(20); // intentionally too large

            int bytesRead = cachingChannel.read(buffer);
            assertEquals(1, bytesRead, "Should only read the last byte, not more.");

            // Seek beyond EOF
            cachingChannel.position(10);
            bytesRead = cachingChannel.read(buffer);
            assertEquals(-1, bytesRead, "Reading beyond EOF should return -1.");

            // Seek far beyond EOF
            cachingChannel.position(100);
            bytesRead = cachingChannel.read(buffer);
            assertEquals(-1, bytesRead, "Reading way beyond EOF should still return -1.");
        }
    }

}
