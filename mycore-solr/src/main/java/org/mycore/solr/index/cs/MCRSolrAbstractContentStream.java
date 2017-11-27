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

package org.mycore.solr.index.cs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

/**
 * Wraps objects to be sent to solr in a content stream.
 * 
 * @see ContentStream
 * 
 * @author shermann
 * @author Matthias Eichner
 * */
public abstract class MCRSolrAbstractContentStream<T> extends ContentStreamBase {

    static final Logger LOGGER = LogManager.getLogger(MCRSolrAbstractContentStream.class);

    protected boolean setup;

    protected InputStream inputStream;

    protected InputStreamReader streamReader;

    protected T source;

    public MCRSolrAbstractContentStream() {
        this(null);
    }

    public MCRSolrAbstractContentStream(T source) {
        this.inputStream = null;
        this.streamReader = null;
        this.setup = false;
        this.source = source;
    }

    @Override
    public InputStream getStream() throws IOException {
        doSetup();
        return this.inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Sets certain properties on a contentStream object. Subclasses must override this method.
     * <p>Its important to call the following setter methods:
     * </p>
     * <ul>
     * <li>setName</li>
     * <li>setSize</li>
     * <li>setSourceInfo</li>
     * <li>setContentType</li>
     * <li>setInputStream</li></ul>
     */
    protected abstract void setup() throws IOException;

    /**
     * Required for {@link #getReader()} to transform any InputStream into a Reader.
     * @return null, will default to "UTF-8"
     */
    protected Charset getCharset() {
        return null;
    }

    /**
     * Checks if the content stream is already set up and ready to use.
     * 
     * @return true if set up.
     */
    public boolean isSetup() {
        return this.setup;
    }

    private void doSetup() throws IOException {
        if (!isSetup()) {
            setup();
            this.setup = true;
        }
    }

    @Override
    public Reader getReader() throws IOException {
        doSetup();
        if (this.streamReader == null) {
            Charset cs = getCharset();
            if (cs == null) {
                cs = StandardCharsets.UTF_8;
            }
            this.streamReader = new InputStreamReader(getStream(), cs);
        }
        return this.streamReader;
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

    public T getSource() {
        return source;
    }

}
