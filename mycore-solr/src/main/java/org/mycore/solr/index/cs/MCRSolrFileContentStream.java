package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.IOException;

import org.mycore.datamodel.ifs.MCRFile;

/**
 * Wraps <code>MCRFile</code> objects as solr content stream.
 * 
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrFileContentStream extends MCRSolrAbstractContentStream<MCRFile> {

    /**
     * @param file
     * @throws IOException
     */
    public MCRSolrFileContentStream(MCRFile file) throws IOException {
        super(file);
    }

    @Override
    protected void setup() throws IOException {
        MCRFile file = getSource();
        this.setName(file.getAbsolutePath());
        this.setSourceInfo(file.getClass().getSimpleName());
        this.setContentType(file.getContentType().getLabel());
        this.setSize(file.getSize());
        this.setInputStream(new BufferedInputStream(file.getContentAsInputStream()));
    }

}
