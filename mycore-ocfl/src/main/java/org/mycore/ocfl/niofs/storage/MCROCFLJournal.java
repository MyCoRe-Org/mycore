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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A generic, append-only journal for persisting a sequence of events to a file.
 * This class provides a mechanism for durable, crash-safe state management.
 * <p>
 * The journal works with a generic event type {@code <E>}. It requires serialization and deserialization logic
 * to be provided by the client, allowing it to be used for any kind of event or state object that can be
 * represented as a single line of text.
 * </p>
 * <h2>Lifecycle and Usage:</h2>
 * <ol>
 *   <li>
 *       <b>Instantiation:</b> Create an instance with a path to the journal file and serializer/deserializer functions.
 *   </li>
 *   <li>
 *       <b>Opening:</b> Call {@link #open(JournalEntry.StateRebuilder)} at application startup. This reads the existing
 *       journal file, line by line, and uses the provided {@code StateRebuilder} to reconstruct the application's
 *       in-memory state.
 *   </li>
 *   <li>
 *       <b>Appending:</b> As the application state changes, call {@link #append(Object)} to log each change as a new
 *       entry. This operation is thread-safe and flushes to disk immediately.
 *   </li>
 *   <li>
 *       <b>Compacting (Optional):</b> Periodically, call {@link #compact(JournalEntry.StateSnapshotter)} to replace the
 *       long, append-only log with a new, smaller journal that represents the complete current state. This prevents the
 *       journal from growing indefinitely and speeds up the "open" phase on subsequent startups.
 *   </li>
 *   <li>
 *       <b>Closing:</b> Call {@link #close()} on application shutdown to ensure the file writer is properly closed.
 *   </li>
 * </ol>
 *
 * @param <E> The type of the event or entry object to be stored in the journal.
 */
public class MCROCFLJournal<E> implements Closeable {

    private final Path journalFile;

    private final JournalEntry.Serializer<E> serializer;

    private final JournalEntry.Deserializer<E> deserializer;

    private BufferedWriter journalWriter;

    /**
     * Constructs a new journal instance.
     *
     * @param journalFile  The path to the file where the journal will be stored.
     * @param serializer   A function to convert an entry of type {@code E} to a {@code String}.
     * @param deserializer A function to convert a {@code String} back to an entry of type {@code E}.
     */
    public MCROCFLJournal(Path journalFile, JournalEntry.Serializer<E> serializer,
        JournalEntry.Deserializer<E> deserializer) {
        this.journalFile = journalFile;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    /**
     * Opens the journal for writing and replays existing entries to rebuild the application's state.
     * If the journal file does not exist, it will be created.
     *
     * @param rebuilder A consumer that applies a deserialized entry to the application's state.
     *                  This is called for each line in the existing journal file.
     * @throws IOException if an I/O error occurs while reading or opening the file.
     * @throws JournalEntry.DeserializationException if a line in the journal is corrupt and cannot be parsed.
     */
    public void open(JournalEntry.StateRebuilder<E> rebuilder) throws IOException {
        if (Files.exists(journalFile)) {
            try (Stream<String> lines = Files.lines(journalFile, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    if (!line.isBlank()) {
                        rebuilder.accept(deserializer.deserialize(line));
                    }
                });
            }
        }
        // Open the journal file for appending new entries
        this.journalWriter = Files.newBufferedWriter(journalFile, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Appends a new entry to the journal. This operation is synchronized and thread-safe.
     * The new entry is immediately flushed to the underlying file to ensure durability.
     *
     * @param entry The entry to log.
     * @throws IOException if the write operation fails or if the journal is not open.
     */
    public synchronized void append(E entry) throws IOException {
        if (journalWriter == null) {
            throw new IOException("Journal is not open. Call open() before appending.");
        }
        journalWriter.write(serializer.apply(entry));
        journalWriter.newLine();
        journalWriter.flush();
    }

    /**
     * Compacts the journal by writing the complete current state as a new, clean journal
     * and atomically replacing the old one. This is a safe, thread-safe operation.
     *
     * @param snapshotter A supplier that provides a complete collection of all entries representing
     *                    the current state of the application. The order of entries in the collection
     *                    is preserved in the new journal file.
     * @throws IOException if an I/O error occurs during the compaction process.
     */
    public synchronized void compact(JournalEntry.StateSnapshotter<E> snapshotter) throws IOException {
        Path tempJournalFile = journalFile.resolveSibling(journalFile.getFileName() + ".tmp");

        // 1. Get the full current state from the client
        Collection<E> currentState = snapshotter.get();

        // 2. Write the full state to a temporary file
        try (BufferedWriter tempWriter = Files.newBufferedWriter(tempJournalFile, StandardCharsets.UTF_8)) {
            for (E entry : currentState) {
                tempWriter.write(serializer.apply(entry));
                tempWriter.newLine();
            }
        }

        // 3. Atomically replace the old journal with the new one
        Files.move(tempJournalFile, journalFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        // 4. Re-open the writer on the new, compacted journal file
        this.close();
        this.journalWriter = Files.newBufferedWriter(journalFile, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Closes the journal's writer. It is essential to call this on application shutdown to ensure
     * all resources are released.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
            journalWriter = null;
        }
    }

    /**
     * A container for functional interfaces used by {@link MCROCFLJournal}.
     * This groups the related serialization and state management behaviors together.
     */
    public static final class JournalEntry {

        private JournalEntry() {
        }

        /**
         * A functional interface for serializing a journal entry of type {@code E} into a String.
         * @param <E> The type of the entry.
         */
        @FunctionalInterface
        public interface Serializer<E> extends Function<E, String> {
        }

        /**
         * A functional interface for deserializing a String from the journal file back into an entry of type {@code E}.
         * @param <E> The type of the entry.
         */
        @FunctionalInterface
        public interface Deserializer<E> {
            E deserialize(String line) throws DeserializationException;
        }

        /**
         * A functional interface for applying a single deserialized entry to the application's in-memory state.
         * This is used during the {@link #open(StateRebuilder)} phase to rebuild the state from the journal.
         * @param <E> The type of the entry.
         */
        @FunctionalInterface
        public interface StateRebuilder<E> extends Consumer<E> {
        }

        /**
         * A functional interface for providing a complete snapshot of the application's current state.
         * This is used by the {@link #compact(StateSnapshotter)} method to create a new, clean journal.
         * @param <E> The type of the entry.
         */
        @FunctionalInterface
        public interface StateSnapshotter<E> extends Supplier<Collection<E>> {
        }

        /**
         * An exception thrown when a journal entry cannot be deserialized from a string,
         * typically due to a format error or data corruption.
         */
        public static class DeserializationException extends RuntimeException {

            @Serial
            private static final long serialVersionUID = 1L;

            public DeserializationException(String message, Throwable cause) {
                super(message, cause);
            }
        }
    }

}
