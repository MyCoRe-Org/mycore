/*
 * $Revision$ 
 * $Date$
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

package org.mycore.datamodel.ifs2;

import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.jdom.Element;

/**
 * Represents a directory stored in a file collection, which may contain other
 * files and directories.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRDirectory extends MCRStoredNode {

    /**
     * Create MCRDirectory representing an existing, already stored directory.
     * 
     * @param parent
     *            the parent directory of this directory
     * @param fo
     *            the local directory in the store storing this directory
     */
    protected MCRDirectory(MCRDirectory parent, FileObject fo, Element data) throws Exception {
        super(parent, fo, data);
    }

    /**
     * Creates a new MCRDirectory in the store.
     * 
     * @param parent
     *            the parent directory of this directory
     * @param name
     *            the name of the new directory
     */
    public MCRDirectory(MCRDirectory parent, String name) throws Exception {
        super(parent, name);
        fo.createFolder();
        data.setName("dir");
        getRoot().saveAdditionalData();
    }
    
    private Element getChildData(String name)
    {
      for( Element child : (List<Element>)(data.getChildren()) )
          if( name.equals(child.getAttributeValue("name")) )
              return child;
      return null;
    }

    /**
     * Returns the MCRFile or MCRDirectory that is represented by the given
     * FileObject, which is a direct child of the directory FileObject this
     * MCRDirectory is stored in.
     * 
     * @return an MCRFile or MCRDirectory child
     */
    protected MCRStoredNode buildChildNode(FileObject fo) throws Exception {
        if (fo == null)
            return null;
        
        Element childData = getChildData(fo.getName().getBaseName());
        if (fo.getType().equals(FileType.FILE))
            return new MCRFile(this, fo, childData);
        else
            return new MCRDirectory(this, fo, childData);
    }
}
