/**
 * 
 */
package org.mycore.solr.index.cs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.datamodel.niofs.MCRContentTypes;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrPathContentStream extends MCRSolrAbstractContentStream<Path> {

    private BasicFileAttributes attrs;

    public MCRSolrPathContentStream(Path path, BasicFileAttributes attrs) {
        super(path);
        this.attrs = attrs;
    }

    @Override
    protected void setup() throws IOException {
        Path file = getSource();
        this.setName(file.toString());
        this.setSourceInfo(file.getClass().getSimpleName());
        this.setContentType(MCRContentTypes.probeContentType(file));
        this.setSize(attrs.size());
        this.setInputStream(Files.newInputStream(file));
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
    }

}
