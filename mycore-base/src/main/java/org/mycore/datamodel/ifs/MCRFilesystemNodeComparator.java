/**
 * 
 */
package org.mycore.datamodel.ifs;

import java.util.Comparator;


/**
 * @author shermann
 *
 */
public class MCRFilesystemNodeComparator implements Comparator<MCRFilesystemNode> {
    @Override
    public int compare(MCRFilesystemNode f1, MCRFilesystemNode f2) {
        if (f1.getName().compareTo(f2.getName()) > 1) {
            return 1;
        }
        if (f1.getName().compareTo(f2.getName()) < 1) {
            return -1;
        }
        return 0;
    }
}
