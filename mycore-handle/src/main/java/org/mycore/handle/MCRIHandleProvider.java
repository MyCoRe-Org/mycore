/**
 * 
 */
package org.mycore.handle;

import org.mycore.datamodel.ifs.MCRFile;


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
