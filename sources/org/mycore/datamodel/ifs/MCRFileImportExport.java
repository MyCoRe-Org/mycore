/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import java.util.*;
import java.io.*;

/**
 * 
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFileImportExport
{
  public static MCRDirectory importFiles( File local, String ownerID, String dirName )
    throws IOException
  {
    MCRArgumentChecker.ensureNotEmpty( ownerID, "path" );
    MCRArgumentChecker.ensureNotEmpty( dirName, "path" );
    
    MCRDirectory dir = new MCRDirectory( dirName, ownerID );
    importFiles( local, dir );
    return dir;
  }
  
  public static void importFiles( File local, MCRDirectory dir )
    throws IOException
  {
    MCRArgumentChecker.ensureNotNull( local, "local file" );
    
    String path = local.getPath();
    String name = local.getName();
    
    MCRArgumentChecker.ensureIsTrue( local.exists(),  "Not found: "    + path );
    MCRArgumentChecker.ensureIsTrue( local.canRead(), "Not readable: " + path );
        
    if( local.isFile() )
    {
      MCRFilesystemNode existing = dir.getChild( name );
      MCRFile file = null;
      
      if( existing instanceof MCRDirectory )      
      {
        existing.delete();
        existing = null;
      }  
      
      if( existing == null )
        file = new MCRFile( name, dir );
      else 
        file = (MCRFile)existing;
      
      file.setContentFrom( local );
    }
    else
    {
      File[] files = local.listFiles();
      
      for( int i = 0; i < files.length; i++ )
      {
        local = files[ i ];
        name = local.getName();
        
        MCRDirectory internalDir = dir;
        
        if( local.isDirectory() )
        {
          MCRFilesystemNode existing = dir.getChild( name );
          if( existing instanceof MCRFile )
          {
            existing.delete();
            existing = null;
          }
          
          if( existing == null )
            internalDir = new MCRDirectory( name, dir );
          else
            internalDir = (MCRDirectory)existing;
        }  
        
        importFiles( local, internalDir );
      }
    }
  }
  
  public static void exportFiles( MCRDirectory dir, File local )
    throws IOException
  {
    MCRArgumentChecker.ensureNotNull( dir,   "internal directory" );
    MCRArgumentChecker.ensureNotNull( local, "local file"         );
    
    String path = local.getPath();
    MCRArgumentChecker.ensureIsTrue( local.canWrite(), "Not writeable: " + path );
    
    if( local.isFile() ) local = local.getParentFile();
    
    MCRFilesystemNode[] children = dir.getChildren();
    for( int i = 0; i < children.length; i++ )
    {
      if( children[ i ] instanceof MCRFile )
      {
        MCRFile internalFile = (MCRFile)( children[ i ] );
        String name = internalFile.getName();
        
        File localFile = new File( local, name );
        internalFile.getContentTo( localFile ); 
      }
      else
      {
        MCRDirectory internalDir = (MCRDirectory)( children[ i ] );
        String name = internalDir.getName();
        
        File localDir = new File( local, name );
        if( ! localDir.exists() ) localDir.mkdir();
        exportFiles( internalDir, localDir );
      }
    }
  }
}
