package org.mycore.datamodel.ifs2;

import java.util.Comparator;

import org.apache.commons.vfs.FileObject;

class MCRFileObjectComparator implements Comparator<FileObject>{

    @Override
    public int compare(FileObject o1, FileObject o2) {
        String path1 = o1.getName().getBaseName();
        String path2 = o2.getName().getBaseName();
        return path1.compareTo(path2);
    }
    
}