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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLReadableByteChannelTest {

    private static final String TEST_DATA = "This is some test data for the OCFL readable byte channel.";

    protected MCROCFLRepository repository;

    private MCROCFLReadableByteChannel channel;

    @PermutedParam
    private boolean remote;

    @BeforeEach
    public void setUp() {
        ObjectVersionId channelTestId = MCROCFLTestCaseHelper.writeFile(repository, "channelTest", "file", TEST_DATA);
        OcflObjectVersion channelTest = repository.getObject(channelTestId);
        channel = new MCROCFLReadableByteChannel(channelTest.getFile("file"));
    }

    @TestTemplate
    public void testSequentialRead() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        // Read the first 10 bytes
        int bytesRead = channel.read(buffer);
        assertEquals(10, bytesRead, "Expected to read 10 bytes.");
        assertArrayEquals("This is so".getBytes(), buffer.array(), "First 10 bytes should match.");
        assertEquals(10, channel.position(), "Position should be updated to 10.");
        assertArrayEquals("This is so".getBytes(), buffer.array(), "First 10 bytes should match.");

        // Read the next 10 bytes
        buffer.clear();
        bytesRead = channel.read(buffer);
        assertEquals(10, bytesRead, "Expected to read another 10 bytes.");
        assertEquals(20, channel.position(), "Position should be updated to 20.");
        assertArrayEquals("me test da".getBytes(), buffer.array(), "Next 10 bytes should match.");
    }

    @TestTemplate
    public void testNonSequentialRead() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        // Jump to position 10 and read
        channel.position(5);
        int bytesRead = channel.read(buffer);
        assertEquals(5, bytesRead, "Expected to read 5 bytes at position 5.");
        assertArrayEquals("is so".getBytes(), buffer.array(), "Bytes from position 5 should match.");

        // Verify the current position
        assertEquals(10, channel.position(), "Position should be updated to 10.");

        // Jump back to position 0 and read
        channel.position(0);
        buffer.clear();
        bytesRead = channel.read(buffer);
        assertEquals(5, bytesRead, "Expected to read 5 bytes at position 0.");
        assertArrayEquals("This ".getBytes(), buffer.array(), "Bytes from position 0 should match.");
    }

    @TestTemplate
    public void testUnsupportedOperations() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        // Test that write throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> channel.write(buffer),
            "Expected UnsupportedOperationException for write operation.");
        // Test that truncate throws UnsupportedOperationException
        assertThrows(
            UnsupportedOperationException.class, () -> channel.truncate(10),
            "Expected UnsupportedOperationException for truncate operation.");
    }

    @TestTemplate
    public void testCloseAndAccess() throws IOException {
        // Close the channel
        channel.close();
        assertFalse(channel.isOpen(), "Channel should be closed.");
        // Test that operations on a closed channel throw ClosedChannelException
        assertThrows(
            ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(10)),
            "Expected ClosedChannelException for read operation.");
        assertThrows(
            ClosedChannelException.class, () -> channel.position(),
            "Expected ClosedChannelException for position operation.");
    }

    @TestTemplate
    public void testSize() throws IOException {
        // Verify the size of the file
        long size = channel.size();
        assertEquals(TEST_DATA.length(), size, "Expected size of the file.");
    }

}
