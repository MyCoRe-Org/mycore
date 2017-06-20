package org.mycore.frontend.fileupload;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public abstract class MCRPostUploadFileProcessor {
    public abstract boolean isProcessable(String path);

    public abstract Path processFile(String path, Path tempFileContent, Supplier<Path> tempFileSupplier)
        throws IOException;
}
