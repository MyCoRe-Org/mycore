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

package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * A {@link SeekableByteChannel} that executes a callback on close.
 */
public class MCROCFLClosableCallbackChannel implements SeekableByteChannel {

    private final SeekableByteChannel delegate;

    private final AfterCloseCallback afterCloseCallback;

    /**
     * Constructs a new {@code ClosableCallbackChannel}.
     *
     * @param seekableByteChannel The underlying {@link SeekableByteChannel}.
     * @param afterCloseCallback The callback to execute after the channel was closed.
     */
    public MCROCFLClosableCallbackChannel(SeekableByteChannel seekableByteChannel,
        AfterCloseCallback afterCloseCallback) {
        this.delegate = seekableByteChannel;
        this.afterCloseCallback = afterCloseCallback;
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        return delegate.read(byteBuffer);
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        return delegate.write(byteBuffer);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long l) throws IOException {
        return delegate.position(l);
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long l) throws IOException {
        return delegate.truncate(l);
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        afterCloseCallback.afterClose();
    }

    @FunctionalInterface
    public interface AfterCloseCallback {

        void afterClose() throws IOException;

    }

}
