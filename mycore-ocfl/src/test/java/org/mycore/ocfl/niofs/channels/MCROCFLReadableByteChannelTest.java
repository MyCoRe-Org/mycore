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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.ocfl.MCROCFLTestCase;
import org.mycore.ocfl.MCROCFLTestCaseHelper;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

public class MCROCFLReadableByteChannelTest extends MCROCFLTestCase {

    private static final String TEST_DATA = "This is some test data for the OCFL readable byte channel.";

    private MCROCFLReadableByteChannel channel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ObjectVersionId channelTestId = MCROCFLTestCaseHelper.writeFile(repository, "channelTest", "file", TEST_DATA);
        OcflObjectVersion channelTest = repository.getObject(channelTestId);
        channel = new MCROCFLReadableByteChannel(channelTest.getFile("file"));
    }

    @Test
    public void testSequentialRead() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        // Read the first 10 bytes
        int bytesRead = channel.read(buffer);
        assertEquals("Expected to read 10 bytes.", 10, bytesRead);
        assertArrayEquals("First 10 bytes should match.", "This is so".getBytes(), buffer.array());
        assertEquals("Position should be updated to 10.", 10, channel.position());
        assertArrayEquals("First 10 bytes should match.", "This is so".getBytes(), buffer.array());

        // Read the next 10 bytes
        buffer.clear();
        bytesRead = channel.read(buffer);
        assertEquals("Expected to read another 10 bytes.", 10, bytesRead);
        assertEquals("Position should be updated to 20.", 20, channel.position());
        assertArrayEquals("Next 10 bytes should match.", "me test da".getBytes(), buffer.array());
    }

    @Test
    public void testNonSequentialRead() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        // Jump to position 10 and read
        channel.position(5);
        int bytesRead = channel.read(buffer);
        assertEquals("Expected to read 5 bytes at position 5.", 5, bytesRead);
        assertArrayEquals("Bytes from position 5 should match.", "is so".getBytes(), buffer.array());

        // Verify the current position
        assertEquals("Position should be updated to 10.", 10, channel.position());

        // Jump back to position 0 and read
        channel.position(0);
        buffer.clear();
        bytesRead = channel.read(buffer);
        assertEquals("Expected to read 5 bytes at position 0.", 5, bytesRead);
        assertArrayEquals("Bytes from position 0 should match.", "This ".getBytes(), buffer.array());
    }

    @Test
    public void testUnsupportedOperations() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        // Test that write throws UnsupportedOperationException
        Assert.assertThrows("Expected UnsupportedOperationException for write operation.",
            UnsupportedOperationException.class, () -> channel.write(buffer));
        // Test that truncate throws UnsupportedOperationException
        Assert.assertThrows("Expected UnsupportedOperationException for truncate operation.",
            UnsupportedOperationException.class, () -> channel.truncate(10));
    }

    @Test
    public void testCloseAndAccess() throws IOException {
        // Close the channel
        channel.close();
        assertFalse("Channel should be closed.", channel.isOpen());
        // Test that operations on a closed channel throw ClosedChannelException
        Assert.assertThrows("Expected ClosedChannelException for read operation.",
            ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(10)));
        Assert.assertThrows("Expected ClosedChannelException for position operation.",
            ClosedChannelException.class, () -> channel.position());
    }

    @Test
    public void testSize() throws IOException {
        // Verify the size of the file
        long size = channel.size();
        assertEquals("Expected size of the file.", TEST_DATA.length(), size);
    }

}
