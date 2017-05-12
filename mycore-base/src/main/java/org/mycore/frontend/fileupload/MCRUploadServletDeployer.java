/**
 * 
 */
package org.mycore.frontend.fileupload;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration.Dynamic;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import com.google.zxing.maxicode.MaxiCodeReader;

/**
 * Uses <code>mycore.properties</code> to configure {@link MCRUploadViaFormServlet}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUploadServletDeployer implements AutoExecutable {

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return MCRUploadViaFormServlet.class.getSimpleName() + " Deployer";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(javax.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            Dynamic uploadServlet = servletContext.addServlet("MCRUploadViaFormServlet", MCRUploadViaFormServlet.class);
            uploadServlet.addMapping("/servlets/MCRUploadViaFormServlet");
            uploadServlet.setMultipartConfig(getMultipartConfig());
        }
    }

    private MultipartConfigElement getMultipartConfig() {
        String location = MCRConfiguration2.getStringOrThrow("MCR.FileUpload.TempStoragePath");
        long maxFileSize = MCRConfiguration2.getLong("MCR.FileUpload.MaxSize").orElse(5000000l);
        long maxRequestSize = maxFileSize;
        int fileSizeThreshold = MCRConfiguration2.getInt("MCR.FileUpload.MemoryThreshold").orElse(1000000);
        LogManager.getLogger()
            .info(() -> MCRUploadViaFormServlet.class.getSimpleName() + " accept files and requests up to "
                + maxRequestSize + " bytes and uses " + location + " as tempory storage for files larger "
                + fileSizeThreshold + " bytes.");
        return new MultipartConfigElement(location, maxFileSize, maxRequestSize,
            fileSizeThreshold);
    }

}
