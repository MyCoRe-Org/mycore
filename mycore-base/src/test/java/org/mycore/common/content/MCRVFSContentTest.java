/*
 * $Id$
 * $Revision: 5697 $ $Date: 11.09.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.content;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRVFSContentTest extends MCRTestCase {

    private static final String TEST_BASE = "/MCRContentTest/";

    private static FileSystemManager FS_MANAGER;

    private static URL TEST_RESOURCE;

    private FileObject fileObject;

    @BeforeClass
    public static void setUpClass() throws FileSystemException {
        FS_MANAGER = VFS.getManager();
        TEST_RESOURCE = MCRVFSContentTest.class.getResource(TEST_BASE + "testDoc.xml");
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        fileObject = FS_MANAGER.resolveFile(TEST_RESOURCE.toString());
        System.out.println(fileObject.getName() + " is open: " + fileObject.isContentOpen());
        System.out.println(MCRVFSContentTest.class.getResource(TEST_BASE + "test.xsl"));
        super.setUp();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.mycore.common.content.MCRContent#getSource()}.
     * @throws IOException 
     * @throws TransformerException 
     */
    @Test
    public final void testGetSource() throws IOException, TransformerException {
        CommonVFSResolver resolver = new CommonVFSResolver(fileObject);
        assertFalse("File is open", resolver.isContentOpen());
        //identity transformation
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StreamResult result = new StreamResult(System.out);
        transformer.transform(resolver.resolve("test://test", null), result);
        assertFalse("File is open after identity transformation", resolver.isContentOpen());
        //simple transformation
        URL xslURL = MCRVFSContentTest.class.getResource(TEST_BASE + "test.xsl");
        URL xmlURL = MCRVFSContentTest.class.getResource(TEST_BASE + "test.xml");
        Source xsl = new StreamSource(xslURL.toString());
        Source xml = new StreamSource(xmlURL.toString());
        transformer = TransformerFactory.newInstance().newTransformer(xsl);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setURIResolver(resolver);
        transformer.transform(xml, result);
        assertFalse("File is open after simple transformation", resolver.isContentOpen());
        //cacheable transformation
        Templates templates = TransformerFactory.newInstance().newTemplates(xsl);
        transformer = templates.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setURIResolver(resolver);
        transformer.transform(xml, result);
        assertFalse("File is open after cacheable transformation", resolver.isContentOpen());
    }

    private static class CommonVFSResolver implements URIResolver {
        private FileObject fo;

        private MCRVFSContent content;

        public CommonVFSResolver(FileObject fo) throws IOException {
            this.fo = fo;
            this.content = new MCRVFSContent(fo);
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            System.out.println("Resolving " + href + " for base " + base);
            try {
                return content.getSource();
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }

        public boolean isContentOpen() {
            return fo.isContentOpen();
        }
    }

}
