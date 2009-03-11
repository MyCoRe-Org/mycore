package org.mycore.datamodel.ifs2;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;

/**
 * JUnit test for MCRDirectory
 * 
 * @author Frank Lützenkirchen
 */
public class MCRDirectoryTest extends MCRTestCase {

    private static String path;

    private MCRFileStore store;

    private MCRFileCollection col;

    protected void setUp() throws Exception {
        super.setUp();

        if (path == null) {
            File temp = File.createTempFile("base", "");
            path = temp.getAbsolutePath();
            temp.delete();

            setProperty("MCR.IFS2.FileStore.TEST.BaseDir", path, true);
            setProperty("MCR.IFS2.FileStore.TEST.SlotLayout", "4-2-2", true);
        }
        VFS.getManager().resolveFile(path).createFolder();

        store = MCRFileStore.getStore("TEST");
        col = store.create();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(path).delete(Selectors.SELECT_ALL);
    }

    public void testName() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "foo");
        Date created = dir.getLastModified();
        assertEquals(dir.getName(), "foo");
        synchronized (this) {
            wait(1000);
        }
        dir.renameTo("bar");
        assertEquals(dir.getName(), "bar");
        assertTrue(dir.getLastModified().after(created));
    }

    public void testSetLastModified() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "foo");
        Date other = new Date(2009, 1, 1);
        dir.setLastModified(other);
        assertEquals(dir.getLastModified(), other);
    }

    public void testType() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "foo");
        assertFalse(dir.isFile());
        assertTrue(dir.isDirectory());
    }

    public void testLabels() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "foo");
        assertTrue(dir.getLabels().isEmpty());
        assertNull(dir.getCurrentLabel());
        dir.setLabel("de", "deutsch");
        dir.setLabel("en", "english");
        String curr = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = dir.getLabel(curr);
        assertEquals(dir.getCurrentLabel(), label);
        assertEquals(dir.getLabels().size(), 2);
        assertEquals(dir.getLabel("en"), "english");
        MCRFileCollection col2 = store.retrieve(col.getID());
        MCRDirectory child = (MCRDirectory) (col2.getChild("foo"));
        assertEquals(child.getLabels().size(), 2);
        dir.clearLabels();
        assertTrue(dir.getLabels().isEmpty());
    }

    public void testGetSize() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "foo");
        assertEquals(dir.getSize(), 0);
    }

    public void testChildren() throws Exception {
        MCRDirectory parent = new MCRDirectory(col, "foo");
        assertFalse(parent.hasChildren());
        assertEquals(parent.getNumChildren(), 0);
        assertNotNull(parent.getChildren());
        assertEquals(parent.getChildren().size(), 0);
        assertNull(parent.getChild("bar"));

        new MCRDirectory(parent, "bar");
        assertTrue(parent.hasChildren());
        assertEquals(parent.getNumChildren(), 1);
        assertNotNull(parent.getChildren());
        assertEquals(parent.getChildren().size(), 1);
        assertNotNull(parent.getChild("bar"));
        MCRNode fromList = parent.getChildren().get(0);
        assertTrue(fromList instanceof MCRDirectory);
        assertEquals(fromList.getName(), "bar");

        new MCRFile(parent, "readme.txt");
        assertTrue(parent.hasChildren());
        assertEquals(parent.getNumChildren(), 2);
        assertNotNull(parent.getChildren());
        assertEquals(parent.getChildren().size(), 2);
        assertNotNull(parent.getChild("bar"));
        assertNotNull(parent.getChild("readme.txt"));
        List<MCRNode> children = parent.getChildren();
        assertFalse(children.get(0).getName().equals(children.get(1).getName()));
        assertTrue("bar readme.txt".contains(children.get(0).getName()));
        assertTrue("bar readme.txt".contains(children.get(1).getName()));
    }

    public void testPath() throws Exception {
        assertEquals(col.getPath(), "/");
        MCRDirectory parent = new MCRDirectory(col, "dir");
        assertEquals(parent.getPath(), "/dir");
        MCRDirectory child = new MCRDirectory(parent, "subdir");
        assertEquals(child.getPath(), "/dir/subdir");
        MCRFile file = new MCRFile(child, "readme.txt");
        assertEquals(file.getPath(), "/dir/subdir/readme.txt");
    }

    public void testNavigationUpwards() throws Exception {
        assertEquals(col.getRoot(), col);
        assertNull(col.getParent());
        MCRDirectory dir = new MCRDirectory(col, "dir");
        assertEquals(dir.getRoot(), col);
        assertEquals(dir.getParent(), col);
        MCRDirectory subdir = new MCRDirectory(dir, "subdir");
        assertEquals(subdir.getRoot(), col);
        assertEquals(subdir.getParent(), dir);
        MCRFile file = new MCRFile(subdir, "readme.txt");
        assertEquals(file.getRoot(), col);
        assertEquals(file.getParent(), subdir);
    }

    public void testGetNodeByPath() throws Exception {
        MCRNode node = col.getNodeByPath("/");
        assertSame(node,col);
        node = col.getNodeByPath(".");
        assertNotNull(node);
        assertSame(node,col);
        MCRDirectory dir = new MCRDirectory(col, "dir");
        node = col.getNodeByPath("dir");
        assertNotNull(node);
        assertEquals(node.getName(),"dir");
        node = col.getNodeByPath("./dir");
        assertNotNull(node);
        assertEquals(node.getName(),"dir");
        node = dir.getNodeByPath("..");
        assertNotNull(node);
        assertSame(node,col);
        MCRDirectory subdir = new MCRDirectory(dir, "subdir");
        node = subdir.getNodeByPath("/");
        assertNotNull(node);
        assertSame(node,col);
        node = subdir.getNodeByPath("../subdir");
        assertNotNull(node);
        assertEquals(node.getName(),"subdir");
        node = subdir.getNodeByPath("../..");
        assertNotNull(node);
        assertSame(node,col);
        node = col.getNodeByPath("./dir/subdir");
        assertNotNull(node);
        assertEquals(node.getName(),"subdir");
    }

    public void testDelete() throws Exception {
        MCRDirectory dir = new MCRDirectory(col, "dir");
        MCRDirectory subdir = new MCRDirectory(dir, "subdir");
        new MCRFile(subdir, "readme.txt");
        dir.delete();
        assertEquals(col.getNumChildren(), 0);
        assertFalse(col.hasChildren());
    }
}
