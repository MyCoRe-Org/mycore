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

package org.mycore.common.content.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSeekableChannelContent;
import org.mycore.common.content.MCRVFSContent;

final class ContentUtils {
    static final int MIN_BUFFER_SIZE = 512;

    static final int DEFAULT_BUFFER_SIZE = 65536;

    static final ArrayList<Range> FULL = new ArrayList<>();

    static final String MIME_BOUNDARY = "MYCORE_MIME_BOUNDARY";

    private static Logger LOGGER = LogManager.getLogger();

    static long copyChannel(final ReadableByteChannel src, final WritableByteChannel dest, final int bufferSize)
        throws IOException {
        if (src instanceof FileChannel) {
            return copyFileChannel((FileChannel) src, dest, bufferSize);
        }
        long bytes = 0;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();

            // write to the channel, may block
            bytes += dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }
        // EOF will leave buffer in fill state
        buffer.flip();
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            bytes += dest.write(buffer);
        }
        return bytes;
    }

    static long copyFileChannel(final FileChannel src, final WritableByteChannel dest, final int bufferSize)
        throws IOException {
        long bytes = 0L;
        long time = -System.currentTimeMillis();
        long size = src.size();
        while (bytes < size) {
            long bytesToTransfer = Math.min(bufferSize, size - bytes);
            long bytesTransfered = src.transferTo(bytes, bytesToTransfer, dest);

            bytes += bytesTransfered;

            if (LOGGER.isDebugEnabled()) {
                long percentage = Math.round(bytes / ((double) size) * 100.0);
                LOGGER.debug("overall bytes transfered: {} progress {}%", bytes, percentage);
            }

        }

        if (LOGGER.isDebugEnabled()) {
            time += System.currentTimeMillis();
            double kBps = (bytes / 1024.0) / (time / 1000.0);
            LOGGER.debug("Transfered: {} bytes in: {} s -> {} kbytes/s", bytes, time / 1000.0, kBps);
        }
        return bytes;
    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     */
    static void copy(final MCRContent content, final OutputStream out, final int inputBufferSize,
        final int outputBufferSize) throws IOException {
        final long bytesCopied;
        long length = content.length();
        if (content instanceof MCRSeekableChannelContent) {
            try (SeekableByteChannel byteChannel = ((MCRSeekableChannelContent) content).getSeekableByteChannel();
                WritableByteChannel nout = Channels.newChannel(out)) {
                endCurrentTransaction();
                bytesCopied = copyChannel(byteChannel, nout, outputBufferSize);
            }
        } else {
            try (InputStream contentIS = content.getInputStream();
                final InputStream in = isInputStreamBuffered(contentIS, content) ? contentIS
                    : new BufferedInputStream(
                        contentIS, inputBufferSize)) {
                endCurrentTransaction();
                // Copy the inputBufferSize stream to the outputBufferSize stream
                bytesCopied = IOUtils.copyLarge(in, out, new byte[outputBufferSize]);
            }
        }
        if (length >= 0 && length != bytesCopied) {
            throw new EOFException("Bytes to send: " + length + " actual: " + bytesCopied);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Wrote {} bytes.", bytesCopied);
        }
    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     *
     * @param content      The MCRContent resource to serve
     * @param out       The outputBufferSize stream to write to
     * @param ranges        Enumeration of the ranges in ascending non overlapping order the client wanted to retrieve
     * @param contentType   Content type of the resource
     */
    static void copy(final MCRContent content, final OutputStream out, final Iterator<Range> ranges,
        final String contentType, final int inputBufferSize, final int outputBufferSize) throws IOException {

        IOException exception = null;

        long lastByte = 0;
        final String endOfLine = "\r\n";

        try (final InputStream resourceInputStream = content.getInputStream();
            final InputStream in = isInputStreamBuffered(resourceInputStream, content) ? resourceInputStream
                : new BufferedInputStream(resourceInputStream, inputBufferSize)) {
            endCurrentTransaction();
            while (exception == null && ranges.hasNext()) {

                final Range currentRange = ranges.next();
                StringBuilder mimeHeader = new StringBuilder();

                // Writing MIME header.
                mimeHeader.append(endOfLine)
                    .append("--")
                    .append(MIME_BOUNDARY)
                    .append(endOfLine);
                if (contentType != null) {
                    mimeHeader.append("Content-Type: ")
                        .append(contentType)
                        .append(endOfLine);
                }
                mimeHeader.append("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/"
                    + currentRange.length)
                    .append(endOfLine)
                    .append(endOfLine);
                out.write(mimeHeader.toString().getBytes(StandardCharsets.US_ASCII));
                // Printing content
                exception = copyRange(in, out, lastByte, currentRange.start, currentRange.end, outputBufferSize);
                lastByte = currentRange.end;
            }
        }
        StringBuilder mimeTrailer = new StringBuilder();
        mimeTrailer.append(endOfLine).append("--").append(MIME_BOUNDARY).append("--");
        out.write(mimeTrailer.toString().getBytes(StandardCharsets.US_ASCII));

        // Rethrow any exception that has occurred
        if (exception != null) {
            throw exception;
        }

    }

    /**
     * Consumes the content and writes it to the ServletOutputStream.
     *
     * @param content  The source resource
     * @param out   The outputBufferSize stream to write to
     * @param range     Range the client wanted to retrieve
     */
    static void copy(final MCRContent content, final OutputStream out, final Range range,
        // TODO: beautify this
        final int inputBufferSize, final int outputBufferSize) throws IOException {
        if (content.isReusable()) {
            try (ReadableByteChannel readableByteChannel = content.getReadableByteChannel()) {
                if (readableByteChannel instanceof SeekableByteChannel) {
                    endCurrentTransaction();
                    SeekableByteChannel seekableByteChannel = (SeekableByteChannel) readableByteChannel;
                    seekableByteChannel.position(range.start);
                    long bytesToCopy = range.end - range.start + 1;
                    while (bytesToCopy > 0) {
                        ByteBuffer byteBuffer;
                        if (bytesToCopy > (long) DEFAULT_BUFFER_SIZE) {
                            byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                        } else {
                            byteBuffer = ByteBuffer.allocate((int) bytesToCopy);
                        }

                        int bytesRead = seekableByteChannel.read(byteBuffer);
                        bytesToCopy -= bytesRead;
                        out.write(byteBuffer.array());
                    }
                    return;
                }
            }
        }

        try (final InputStream resourceInputStream = content.getInputStream();
            final InputStream in = isInputStreamBuffered(resourceInputStream, content) ? resourceInputStream
                : new BufferedInputStream(resourceInputStream, inputBufferSize)) {
            endCurrentTransaction();
            final IOException exception = copyRange(in, out, 0, range.start, range.end, outputBufferSize);
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * Copy the content with the specified range of bytes from InputStream to OutputStream.
     *
     * @param in The input
     * @param out The output
     * @param inPosition Current position of the input
     * @param start Start position to be copied
     * @param end End position to be copied
     * @return Exception which occurred during processing or if less than <code>end - start + 1</code> bytes were read/written.
     */
    static IOException copyRange(final InputStream in, final OutputStream out, final long inPosition,
        final long start, final long end, final int outputBufferSize) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Serving bytes:{}-{}", start, end);
        }

        final long bytesToRead = end - start + 1;
        final long skip = start - inPosition;
        try {
            final long copied = copyLarge(in, out, skip, bytesToRead, new byte[outputBufferSize]);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Served bytes:{}", copied);
            }
            if (copied != bytesToRead) {
                return new EOFException("Bytes to send: " + bytesToRead + " actual: " + copied);
            }
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    static long copyLarge(InputStream input, OutputStream output, long inputOffset, long length, byte[] buffer)
        throws IOException {
        if (inputOffset > 0L) {
            long bytesToSkip = inputOffset;
            while (bytesToSkip > 0) {
                bytesToSkip -= input.skip(bytesToSkip);
            }
        }

        if (length == 0L) {
            return 0L;
        } else {
            int bufferLength = buffer.length;
            int bytesToRead = bufferLength;
            if (length > 0L && length < (long) bufferLength) {
                bytesToRead = (int) length;
            }

            long totalRead = 0L;

            int read;
            while (bytesToRead > 0 && -1 != (read = input.read(buffer, 0, bytesToRead))) {
                output.write(buffer, 0, read);
                totalRead += (long) read;
                if (length > 0L) {
                    bytesToRead = (int) Math.min(length - totalRead, (long) bufferLength);
                }
            }

            return totalRead;
        }
    }

    /**
     * Called before sending data to end hibernate transaction.
     */
    static void endCurrentTransaction() {
        MCRSessionMgr.getCurrentSession().commitTransaction();
    }

    /**
     * Returns if the content InputStream is already buffered.
     * @param contentIS output of {@link MCRContent#getInputStream()}
     * @param content MCRContent instance associated with contentIS
     */
    static boolean isInputStreamBuffered(final InputStream contentIS, final MCRContent content) {
        return contentIS instanceof BufferedInputStream || contentIS instanceof ByteArrayInputStream
            || contentIS instanceof FilterInputStream && content instanceof MCRVFSContent;
    }
}
