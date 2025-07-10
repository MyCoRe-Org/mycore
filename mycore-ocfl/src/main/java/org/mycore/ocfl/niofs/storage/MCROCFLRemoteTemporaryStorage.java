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

package org.mycore.ocfl.niofs.storage;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.mycore.common.digest.MCRDigest;

/**
 * A temporary storage for remote OCFL repositories.
 */
public interface MCROCFLRemoteTemporaryStorage {

    /**
     * Counts the number of cached files.
     *
     * @return number of cached files.
     */
    int count();

    /**
     * Returns the size of this cache in bytes.
     *
     * @return number of allocated bytes.
     */
    long allocated();

    /**
     * Checks if an entry for the given digest exists in the cache.
     *
     * @param digest The digest to check.
     * @return {@code true} if the entry exists, {@code false} otherwise.
     */
    boolean exists(MCRDigest digest);

    /**
     * Clears the entire cache, deleting all temporary files and resetting internal state.
     *
     * @throws IOException if an I/O error occurs while deleting the root storage directory.
     */
    void clear() throws IOException;

    /**
     * Writes a complete byte array to the cache as a new entry.
     *
     * @param originalFileName The original file name used to probe the content type.
     * @param bytes            The content to write.
     * @param options          The options specifying how the file is opened.
     * @return The calculated {@link MCRDigest} of the written content.
     * @throws IOException if an I/O error occurs during writing.
     */
    MCRDigest write(String originalFileName, byte[] bytes, OpenOption... options) throws IOException;

    /**
     * Opens a {@link SeekableByteChannel} for reading the content associated with the given digest.
     *
     * @param digest The digest of the content to read.
     * @return A new readable byte channel for the cached file.
     * @throws NoSuchFileException if no entry for the digest exists in the cache.
     * @throws IOException         if an I/O error occurs.
     */
    SeekableByteChannel readByteChannel(MCRDigest digest) throws IOException;

    /**
     * Imports a file from an external path into the cache.
     *
     * @param source The source file which should be copied into the cache.
     * @return The digest of the imported file's content, which serves as its key in the cache.
     * @throws FileAlreadyExistsException if the digest already exists in the cache.
     * @throws IOException                if an I/O error occurs.
     */
    MCRDigest importFile(Path source)
        throws FileAlreadyExistsException, IOException;

    /**
     * Copies a cached file to a target path.
     *
     * @param sourceDigest  The digest of the source file in the cache.
     * @param target  The destination path for the copy.
     * @param options Options specifying how the copy should be done.
     * @throws NoSuchFileException if the source digest is not found in the cache.
     * @throws IOException         if an I/O error occurs during the copy operation.
     */
    void exportFile(MCRDigest sourceDigest, Path target, CopyOption... options) throws IOException;

    /**
     * Probes the content type (MIME type) of a file stored in the cache.
     *
     * @param digest The digest of the file to probe.
     * @return The probed content type as a string (e.g., "image/jpeg").
     * @throws NoSuchFileException if the digest is not found in the cache.
     * @throws IOException         if an I/O error occurs.
     */
    String probeContentType(MCRDigest digest) throws IOException;

    /**
     * Prepares a new entry for writing to the cache transactionally.
     * <p>
     * This method returns a {@link MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter} which provides a channel for
     * streaming data to a temporary file. The caller is responsible for either calling
     * {@link MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter#commit()} to finalize the entry or
     * {@link MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter#abort()} to discard it.
     * <p>
     * <b>Be aware that the caller of this method is responsible for closing the channel!</b>
     *
     * @param originalFileName  The original file name. Used for probing the content type.
     * @param attrs Optional file attributes for the temporary file.
     * @return A writer object to manage the caching operation.
     * @throws IOException if an I/O error occurs creating the temporary file.
     */
    CacheEntryWriter newCacheEntry(String originalFileName, FileAttribute<?>... attrs) throws IOException;

    /**
     * Represents an in-progress write operation to the cache.
     * <p>
     * It provides a channel to a temporary file. The operation can either be committed, which moves the temporary file
     * to its final destination, or aborted, which deletes the temporary file.
     * <p>
     * This class is not thread-safe and is intended for single-threaded use.
     */
    class CacheEntryWriter {

        private final SeekableByteChannel channel;

        private final Path path;

        private final OnCommit onCommit;

        private final OnAbort onAbort;

        /**
         * Constructs a new cache entry writer.
         *
         * @param channel The writable channel to the temporary file.
         * @param onCommit A consumer that is called with the final path upon successful commit.
         */
        public CacheEntryWriter(SeekableByteChannel channel, Path path, OnCommit onCommit, OnAbort onAbort) {
            this.channel = channel;
            this.path = path;
            this.onCommit = onCommit;
            this.onAbort = onAbort;
        }

        /**
         * Returns the writable byte channel to the temporary cache file.
         * The caller should not close this channel directly; it will be closed when this writer is closed.
         */
        public SeekableByteChannel getChannel() {
            return channel;
        }

        public Path getPath() {
            return path;
        }

        /**
         * Commits the write operation.
         */
        public MCRDigest commit() throws IOException {
            return onCommit.commit();
        }

        /**
         * Aborts the write operation.
         */
        public void abort() throws IOException {
            onAbort.abort();
        }

    }

    @FunctionalInterface
    interface OnCommit {
        MCRDigest commit() throws IOException;
    }

    @FunctionalInterface
    interface OnAbort {
        void abort() throws IOException;
    }

}
