package org.mycore.ocfl.niofs.storage;

import org.mycore.datamodel.niofs.MCRVersionedPath;

public abstract class MCROCFLStorageTestCase {

    public static final String DERIVATE_3 = "junit_derivate_00000003";

    protected MCRVersionedPath path1;

    protected MCRVersionedPath path2;

    protected MCRVersionedPath path3;

    public void setUp() throws Exception {
        this.path1 = MCRVersionedPath.head(DERIVATE_3, "file1");
        this.path2 = MCRVersionedPath.head(DERIVATE_3, "file2");
        this.path3 = MCRVersionedPath.head(DERIVATE_3, "file3");
    }

}
