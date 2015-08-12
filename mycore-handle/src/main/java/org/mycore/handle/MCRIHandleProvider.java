/**
 * 
 */
package org.mycore.handle;

import org.mycore.datamodel.niofs.MCRPath;


/**
 * @author shermann
 *
 */
public interface MCRIHandleProvider {

    /** Generates a single handle */
    public MCRHandle generateHandle();

    public MCRHandle requestHandle(MCRPath file);

    /**
     * Generates multiple handles.
     * 
     * @param amount the amount of handles to generate, must be &gt;= 1
     */
    public MCRHandle[] generateHandle(int amount);
}
