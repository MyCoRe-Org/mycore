package org.mycore.solr.index.cs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Wraps objects to be sent to solr in a content stream.
 * 
 * @see {@link ContentStream}
 * 
 * @author shermann
 * */
abstract public class AbstractSolrContentStream extends ContentStreamBase implements Runnable {
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    final static Logger LOGGER = Logger.getLogger(AbstractSolrContentStream.class);

    protected long length;

    protected String name, sourceInfo, contentType;

    protected InputStream inputStream;

    protected Object source;

    protected AbstractSolrContentStream() {
        super();
        length = -1;
        inputStream = null;
    }

    /**
     * Sets certain properties on a contentStream object. Subclasses must overide this method.
     */
    abstract protected void setup();

    /**
     * @return the source object (the one provided to the constructor)
     */
    public Object getSource() {
        return this.source;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Long getSize() {
        if (length == -1) {
            setup();
        }
        return Long.valueOf(length);
    }

    @Override
    public InputStream getStream() throws IOException {
        if (inputStream == null) {
            setup();
        }

        return inputStream;
    }

    @Override
    public Reader getReader() throws IOException {
        if (inputStream == null) {
            setup();
        }
        return new BufferedReader(new InputStreamReader(getStream()));
    }

    /**
     * Closes the underlying reader and input streams.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        getReader().close();
        getStream().close();
    }

    public String toString() {
        return this.name + " (" + sourceInfo + ")";
    }

    /**
     * Invoke this method if you want to index the object asynchronous.
     * */
    @Override
    public void run() {
        Session session = null;
        try {
            session = MCRHIBConnection.instance().getSession();
            session.beginTransaction();
            index();
        } catch (Exception ex) {
            LOGGER.error("Error executing index task for object " + getSourceInfo(), ex);
        } finally {
            try {
                close();
            } catch (IOException e) {
                LOGGER.error("Error closing underlying streams in " + getClass(), e);
            }
            session.close();
        }
    }

    /**
     * Invokes an index request for the current content stream.
     */
    protected abstract void index();
}
