package org.mycore.datamodel.ifs2;

import java.io.IOException;

public class MCRObjectIDFileSystemDate extends MCRObjectIDDateImpl {
    public MCRObjectIDFileSystemDate(MCRStoredMetadata sm, String id) throws IOException {
        super(sm.getLastModified(), id);
    }
}
