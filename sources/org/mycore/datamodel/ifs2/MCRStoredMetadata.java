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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Frank Lützenkirchen
 */
public class MCRStoredMetadata {
    
    protected int id;
    protected FileObject fo;
    protected MCRMetadataStore store;
    
    MCRStoredMetadata( MCRMetadataStore store, FileObject fo, int id )
    {
      this.store = store;
      this.id = id;
      this.fo = fo;
    }
    
    public Document getXML() throws Exception
    {
      InputStream in = fo.getContent().getInputStream();
      Document xml = new SAXBuilder().build(in);
      in.close();
      return xml;
    }
    
    public int getID()
    {
      return id;
    }
    
    public MCRMetadataStore getStore()
    { return store; }
    
    public Date getLastModified() throws Exception
    {
        long time = fo.getContent().getLastModifiedTime();
        return new Date(time);
    }
    
    public void setLastModified( Date date ) throws Exception
    {
        fo.getContent().setLastModifiedTime( date.getTime() );
    }
    
    public void delete() throws Exception
    {
        store.delete(id);
    }
    
    /**
     * Updates the stored XML document
     * 
     * @param xml
     *            the XML document to be stored
     */
    public void update(Document xml) throws Exception {
        save(xml);
    }
    
    void create(Document xml) throws Exception {
        fo.createFile();
        save(xml);
    }
    
    void save(Document xml) throws Exception {
        OutputStream out = fo.getContent().getOutputStream();
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8").setIndent("  "));
        xout.output(xml, out);
        out.close();
    }
}
