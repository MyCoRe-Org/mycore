package org.mycore.datamodel.ifs2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mycore.common.MCRSessionMgr;

/**
 * JUnit test for MCRDirectory
 * 
 * @author Frank Lützenkirchen
 */
public class MCRDirectoryTest extends MCRIFS2TestCase {

    private MCRFileCollection col;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        col = getStore().create();
    }

    @Test
    public void name() throws Exception {
        MCRDirectory dir = col.createDir("foo");
        Date created = dir.getLastModified();
        assertEquals("foo", dir.getName());
        synchronized (this) {
            wait(1000);
        }
        dir.renameTo("bar");
        assertEquals("bar", dir.getName());
        assertTrue(dir.getLastModified().after(created));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setLastModified() throws Exception {
        MCRDirectory dir = col.createDir("foo");
        Date other = new Date(109, 1, 1);
        dir.setLastModified(other);
        assertEquals(other, dir.getLastModified());
    }

    @Test
    public void type() throws Exception {
        MCRDirectory dir = col.createDir("foo");
        assertFalse(dir.isFile());
        assertTrue(dir.isDirectory());
    }

    @Test
    public void labels() throws Exception {
        MCRDirectory dir = col.createDir("foo");
        assertTrue(dir.getLabels().isEmpty());
        assertNull(dir.getCurrentLabel());
        dir.setLabel("de", "deutsch");
        dir.setLabel("en", "english");
        String curr = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = dir.getLabel(curr);
        assertEquals(label, dir.getCurrentLabel());
        assertEquals(2, dir.getLabels().size());
        assertEquals("english", dir.getLabel("en"));
        MCRFileCollection col2 = getStore().retrieve(col.getID());
        MCRDirectory child = (MCRDirectory) col2.getChild("foo");
        assertEquals(2, child.getLabels().size());
        dir.clearLabels();
        assertTrue(dir.getLabels().isEmpty());
    }

    @Test
    public void getSize() throws Exception {
        MCRDirectory dir = col.createDir("foo");
        assertEquals(0, dir.getSize());
    }

    @Test
    public void children() throws Exception {
        MCRDirectory parent = col.createDir("foo");
        assertFalse(parent.hasChildren());
        assertEquals(0, parent.getNumChildren());
        assertNotNull(parent.getChildren());
        assertEquals(0, parent.getChildren().size());
        assertNull(parent.getChild("bar"));

        parent.createDir("bar");
        assertTrue(parent.hasChildren());
        assertEquals(1, parent.getNumChildren());
        assertNotNull(parent.getChildren());
        assertEquals(1, parent.getChildren().size());
        assertNotNull(parent.getChild("bar"));
        MCRNode fromList = parent.getChildren().get(0);
        assertTrue(fromList instanceof MCRDirectory);
        assertEquals("bar", fromList.getName());

        parent.createFile("readme.txt");
        assertTrue(parent.hasChildren());
        assertEquals(2, parent.getNumChildren());
        assertNotNull(parent.getChildren());
        assertEquals(2, parent.getChildren().size());
        assertNotNull(parent.getChild("bar"));
        assertNotNull(parent.getChild("readme.txt"));
        List<MCRNode> children = parent.getChildren();
        assertFalse(children.get(0).getName().equals(children.get(1).getName()));
        assertTrue("bar readme.txt".contains(children.get(0).getName()));
        assertTrue("bar readme.txt".contains(children.get(1).getName()));
    }

    @Test
    public void path() throws Exception {
        assertEquals("/", col.getPath());
        MCRDirectory parent = col.createDir("dir");
        assertEquals("/dir", parent.getPath());
        MCRDirectory child = parent.createDir("subdir");
        assertEquals("/dir/subdir", child.getPath());
        MCRFile file = child.createFile("readme.txt");
        assertEquals("/dir/subdir/readme.txt", file.getPath());
    }

    @Test
    public void navigationUpwards() throws Exception {
        assertEquals(col, col.getRoot());
        assertNull(col.getParent());
        MCRDirectory dir = col.createDir("dir");
        assertEquals(col, dir.getRoot());
        assertEquals(col, dir.getParent());
        MCRDirectory subdir = dir.createDir("subdir");
        assertEquals(col, subdir.getRoot());
        assertEquals(dir, subdir.getParent());
        MCRFile file = subdir.createFile("readme.txt");
        assertEquals(col, file.getRoot());
        assertEquals(subdir, file.getParent());
    }

    @Test
    public void getNodeByPath() throws Exception {
        MCRNode node = col.getNodeByPath("/");
        assertSame(node, col);
        node = col.getNodeByPath(".");
        assertNotNull(node);
        assertSame(node, col);
        MCRDirectory dir = col.createDir("dir");
        node = col.getNodeByPath("dir");
        assertNotNull(node);
        assertEquals("dir", node.getName());
        node = col.getNodeByPath("./dir");
        assertNotNull(node);
        assertEquals("dir", node.getName());
        node = dir.getNodeByPath("..");
        assertNotNull(node);
        assertSame(node, col);
        MCRDirectory subdir = dir.createDir("subdir");
        node = subdir.getNodeByPath("/");
        assertNotNull(node);
        assertSame(node, col);
        node = subdir.getNodeByPath("../subdir");
        assertNotNull(node);
        assertEquals("subdir", node.getName());
        node = subdir.getNodeByPath("../..");
        assertNotNull(node);
        assertSame(node, col);
        node = col.getNodeByPath("./dir/subdir");
        assertNotNull(node);
        assertEquals("subdir", node.getName());
    }

    @Test
    public void delete() throws Exception {
        MCRDirectory dir = col.createDir("dir");
        MCRDirectory subdir = dir.createDir("subdir");
        subdir.createFile("readme.txt");
        dir.delete();
        assertEquals(0, col.getNumChildren());
        assertFalse(col.hasChildren());
    }
}
