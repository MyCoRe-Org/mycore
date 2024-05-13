package org.mycore.datamodel.niofs;

import java.io.IOException;

/**
 * Thrown to indicate that an operation attempted to modify a read-only file or directory.
 */
public class MCRReadOnlyIOException extends IOException {

    /**
     * Constructs a new read-only exception with the specified detail message.
     * The message can be retrieved later by the {@link Throwable#getMessage()} method.
     *
     * @param message The detailed message which explains the reason the exception is thrown.
     */
    public MCRReadOnlyIOException(String message) {
        super(message);
    }

    /**
     * Constructs a new read-only exception with the specified cause. The cause is the
     * underlying reason for this exception.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link Throwable#getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MCRReadOnlyIOException(Throwable cause) {
        super(cause);
    }

}
