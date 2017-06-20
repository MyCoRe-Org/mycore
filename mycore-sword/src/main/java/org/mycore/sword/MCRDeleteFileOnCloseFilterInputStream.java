package org.mycore.sword;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCRDeleteFileOnCloseFilterInputStream extends FilterInputStream {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path fileToDelete;

    public MCRDeleteFileOnCloseFilterInputStream(InputStream source, Path fileToDelete) {
        super(source);
        this.fileToDelete = fileToDelete;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } catch (IOException e) {
            throw e;
        } finally {
            LOGGER.info("Delete File : " + fileToDelete.toString());
            Files.delete(fileToDelete);
        }
    }
}
