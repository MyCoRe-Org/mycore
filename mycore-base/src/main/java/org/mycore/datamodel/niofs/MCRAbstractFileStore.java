/**
 * 
 */
package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;

/**
 * @author Thomas Scheffler
 *
 */
public abstract class MCRAbstractFileStore extends FileStore {

    /**
     * Returns base directory of this filestore.
     */
    public abstract Path getBaseDirectory() throws IOException;

    /**
     * Translates the given path into an absolute path of the physical filesystem.
     * 
     * To retrieve a relative path use {@link Path#relativize(Path)} on {@link #getBaseDirectory()}.
     * The returned path may not exist or may be null.
     */
    public abstract Path getPhysicalPath(MCRPath path);

}
