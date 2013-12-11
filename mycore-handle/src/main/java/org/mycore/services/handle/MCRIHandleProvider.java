/**
 * 
 */
package org.mycore.services.handle;

import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.services.handle.hibernate.tables.MCRHandle;


/**
 * @author shermann
 *
 */
public interface MCRIHandleProvider {

    /** Generates a single handle */
    public MCRHandle generateHandle();

    /**
     * @param file
     * @return
     */
    public MCRHandle requestHandle(MCRFile file);

    /**
     * Generates multiple handles.
     * 
     * @param amount the amount of handles to generate, must be &gt;= 1
     */
    public MCRHandle[] generateHandle(int amount);
}
