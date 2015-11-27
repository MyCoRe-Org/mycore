/**
 * 
 */
package org.mycore.common.xsl;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import org.mycore.common.MCRException;


/**
 * A {@link StreamSource} that offers a lazy initialization to {@link #getInputStream()}.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRLazyStreamSource extends StreamSource {

    private InputStreamSupplier inputStreamSupplier;
    
    private static InputStreamSupplier nullSupplier = new InputStreamSupplier() {

        @Override
        public InputStream get() throws IOException {
            return null;
        }
    };

    public MCRLazyStreamSource(InputStreamSupplier inputStreamSupplier, String systemId) {
        super(systemId);
        this.inputStreamSupplier = inputStreamSupplier == null ? nullSupplier : inputStreamSupplier;
    }

    @Override
    public void setInputStream(final InputStream inputStream) {
        inputStreamSupplier = new InputStreamSupplier() {
            
            @Override
            public InputStream get() throws IOException {
                return inputStream;
            }
        };
    }

    @Override
    public InputStream getInputStream() {
        try {
            return inputStreamSupplier.get();
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }
    
    public static interface InputStreamSupplier{
        public InputStream get() throws IOException;
    }

}
