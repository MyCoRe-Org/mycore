package org.mycore.solr.index.cs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.solr.SolrServerFactory;

/**
 * Wraps objects to be sent to solr in a content stream.
 * 
 * @see {@link ContentStream}
 * 
 * @author shermann
 * */
abstract public class AbstractSolrContentStream<T> extends ContentStreamBase implements Runnable {
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    final static Logger LOGGER = Logger.getLogger(AbstractSolrContentStream.class);

    static String TRANSFORM = null;

    static {
        TRANSFORM = MCRConfiguration.instance().getString("MCR.Module-solr.transform", "object2fields.xsl");
    }

    protected InputStream inputStream;

    protected InputStreamReader streamReader;

    protected boolean setup;

    protected T source;

    protected AbstractSolrContentStream() {
        super();
        inputStream = null;
        streamReader = null;
        setup = false;
    }

    /**
     * Sets certain properties on a contentStream object. Subclasses must overide this method.
     */
    abstract protected void setup() throws IOException;

    /**
     * @return the source object (the one provided to the constructor)
     */
    public T getSource() {
        return this.source;
    }

    private void doSetup() throws IOException {
        if (!setup) {
            setup();
            setup = true;
        }
    }

    @Override
    public InputStream getStream() throws IOException {
        doSetup();
        return inputStream;
    }

    @Override
    public Reader getReader() throws IOException {
        doSetup();
        if (streamReader == null) {
            streamReader = new InputStreamReader(getStream());
        }
        return streamReader;
    }

    @Override
    public Long getSize() {
        try {
            doSetup();
        } catch (IOException e) {
            LOGGER.error("Could not setup content stream.", e);
        }
        return super.getSize();
    }

    public String toString() {
        return this.name + " (" + sourceInfo + ")";
    }

    /**
     * Invokes an index request for the current content stream.
     */
    protected void index() {
        try {
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\"");
            long tStart = System.currentTimeMillis();
            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/xslt");
            updateRequest.addContentStream(this);
            updateRequest.setParam("tr", TRANSFORM);
            SolrServerFactory.getSolrServer().request(updateRequest);
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
        } catch (Exception ex) {
            LOGGER.error("Error sending content to solr through content stream " + this, ex);
        }
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
            session.close();
        }
    }
}
