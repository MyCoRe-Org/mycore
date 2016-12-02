package org.mycore.mets.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.frontend.fileupload.MCRPostUploadFileProcessor;

public class MCRGoobiMetsPostUploadProcessor extends MCRPostUploadFileProcessor {

    private final MCRXSLTransformer goobiMetsTransformer;

    public MCRGoobiMetsPostUploadProcessor() {
        goobiMetsTransformer = MCRXSLTransformer.getInstance("xsl/goobi-mycore-mets.xsl");
    }

    @Override
    public boolean isProcessable(String path) {
        return path.endsWith("mets.xml");
    }

    @Override
    public Path processFile(String path, Path tempFile, Supplier<Path> tempFileSupplier)
        throws IOException {
        try (InputStream in = Files.newInputStream(tempFile)) {
            MCRStreamContent streamContent = new MCRStreamContent(in);
            MCRContent transform = goobiMetsTransformer.transform(streamContent);
            Path result = tempFileSupplier.get();
            try (OutputStream out = Files.newOutputStream(result)) {
                out.write(transform.asByteArray());
                return result;
            }
        }
    }
}
