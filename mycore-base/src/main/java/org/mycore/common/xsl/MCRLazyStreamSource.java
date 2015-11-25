/**
 * 
 */
package org.mycore.common.xsl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRException;


/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRLazyStreamSource extends StreamSource {

    private InputStreamSupplier inputStreamSupplier;

    public MCRLazyStreamSource(InputStreamSupplier inputStreamSupplier, String systemId) {
        super(systemId);
        this.inputStreamSupplier = Optional.ofNullable(inputStreamSupplier).orElse(() -> null);
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        inputStreamSupplier = () -> inputStream;
    }

    @Override
    public InputStream getInputStream() {
        LogManager.getLogger().info(()-> "getInputStream is called ", new IOException("track me"));
        try {
            return inputStreamSupplier.get();
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }
    
    @FunctionalInterface
    public static interface InputStreamSupplier{
        public InputStream get() throws IOException;
    }

}
