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
 * Imports or exports complete directory trees with all 
 * contained files and subdirectories between the local
 * host's filesystem and the internal MCRDirectory structures.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFileImportExport
{
  /**
   * Imports the contents of a local file or directory into
   * a newly created MCRDirectory that is owned by the given 
   * owner ID. The new MCRDirectory will have the same name 
   * as the owner ID.
   * 
   * If the local object is
   * a file, a MCRFile with the same name will be created or
   * updated in that MCRDirectory. If the local object is
   * a directory, all contained subdirectories and files will 
   * be imported into the newly created MCRDirectory. 
   * That means that after finishing this method, the
   * complete directory structure will have been imported and
   * mapped from the local filesystem's structure. The method
   * checks the contents of each local file to be imported. If
   * the file's content has not changed for existing files,
   * the internal MCRFile will not be updated. If there is any
   * exception while importing the local contents, the system will
   * try to undo this operation by completely deleting all
   * content that was imported so far.
   *
   * @param local the local file or directory to be imported
   * @param ownerID the ID of the logical owner of the content that will be stored
   * @return a new MCRDirectory that will contain all imported files and directories as instances of MCRFilesystemNode children.
   **/  
  public static MCRDirectory importFiles( File local, String ownerID )
  {
    MCRArgumentChecker.ensureNotEmpty( ownerID, "owner ID" );

    // Create new parent directory    
    MCRDirectory dir = new MCRDirectory( ownerID, ownerID );
    try // Try to import local content into this new directory
    { importFiles( local, dir ); }
    catch( MCRException mex ) // If anything goes wrong
    {
      try{ dir.delete(); }  // Try to delete all content stored so far
      catch( Exception ignored ){}
      
      throw mex;
    }
    return dir;
  }

  /**
   * Imports the contents of a local file or directory into
   * an existing MCRDirectory that is owned by the given 
   * owner ID. The new MCRDirectory will have the same name 
   * as the owner ID.
   * 
   * If the local object is
   * a file, a MCRFile with the same name will be created or
   * updated in that MCRDirectory. If the local object is
   * a directory, all contained subdirectories and files will 
   * be imported into the newly created MCRDirectory. 
   * That means that after finishing this method, the
   * complete directory structure will have been imported and
   * mapped from the local filesystem's structure. The method
   * checks the contents of each local file to be imported. If
   * the file's content has not changed for existing files,
   * the internal MCRFile will not be updated. If there is any
   * exception while importing the local contents, the system will
   * stop with the last state and break the work.
   *
   * @param local the local file or directory to be imported
   * @param ownerID the ID of the logical owner of the content that will be stored
   * @return a new MCRDirectory that will contain all imported files and directories as instances of MCRFilesystemNode children.
   **/  
  public static MCRDirectory addFiles( File local, String ownerID )
  {
    MCRArgumentChecker.ensureNotEmpty( ownerID, "owner ID" );

    // Get the existing parent directory    
    MCRDirectory dir = MCRDirectory.getRootDirectory( ownerID );
    try // Try to import local content into this new directory
    { importFiles( local, dir ); }
    catch( MCRException mex ) // If anything goes wrong
    { throw mex; }
    return dir;
  }

  /**
   * Imports the contents of a local file or directory into
   * the MyCoRe Internal Filesystem. If the local object is
   * a file, a MCRFile with the same name will be created or
   * updated in the given MCRDirectory. If the local object is
   * a directory, all contained subdirectories and files will 
   * be imported into the given MCRDirectory. 
   * That means that after finishing this method, the
   * complete directory structure will have been imported and
   * mapped from the local filesystem's structure. The method
   * checks the contents of each local file to be imported. If
   * the file's content has not changed for existing files,
   * the internal MCRFile will not be updated. If an internal
   * directory is updated from a local directory, new files will
   * be added, existing files will be updated if necessary, but
   * files that already exist in the given MCRDirectory but not
   * in the local filesystem will be kept and will not be deleted.
   *
   * @param local the local file or directory
   * @param dir an existing MCRDirectory where to store the imported contents of the local filesystem.
   **/  
  public static void importFiles( File local, MCRDirectory dir )
  {
    MCRArgumentChecker.ensureNotNull( local, "local file" );
    
    String path = local.getPath();
    String name = local.getName();
    
    MCRArgumentChecker.ensureIsTrue( local.exists(),  "Not found: "    + path );
    MCRArgumentChecker.ensureIsTrue( local.canRead(), "Not readable: " + path );
        
    if( local.isFile() ) // Import a local file
    {
      MCRFilesystemNode existing = dir.getChild( name );
      MCRFile file = null;
      
      // If internal directory with same name exists
      if( existing instanceof MCRDirectory )      
      { 
        existing.delete(); // delete it
        existing = null;
      }  
      
      if( existing == null ) // Create new, empty MCRFile 
        file = new MCRFile( name, dir );
      else 
      {  
        file = (MCRFile)existing; // Update existing MCRFile

        // Determine MD5 checksum of local file 
        FileInputStream fin = null;
        try{ fin = new FileInputStream( local ); }
        catch( FileNotFoundException willNotBeThrown ){}
        
        MCRContentInputStream cis = new MCRContentInputStream( fin );
        
        if (! MCRUtils.copyStream( cis, null ) )
        {
          String msg = "Error while reading local file " + local.getPath();
          throw new MCRException( msg );
        }
        
        String local_md5 = cis.getMD5String();
        
        // If file content of local file has not changed, do not load it again
        if( file.getMD5().equals( local_md5 ) ) return;
      }
      
      // Store file content
      file.setContentFrom( local );
    }
    else
    {
      File[] files = local.listFiles();
      
      // For each local child node
      for( int i = 0; i < files.length; i++ )
      {
        local = files[ i ];
        name = local.getName();
        
        MCRDirectory internalDir = dir;
        
        if( local.isDirectory() )
        {
          MCRFilesystemNode existing = dir.getChild( name );
          if( existing instanceof MCRFile )
          { // If there is an existing MCRFile with same name
            existing.delete(); // delete that existing MCRFile
            existing = null;
          }
          
          if( existing == null ) // Create new directory
            internalDir = new MCRDirectory( name, dir );
          else 
            internalDir = (MCRDirectory)existing;
        }  
        
        importFiles( local, internalDir ); // Recursively import
      }
    }
  }

  /**
   * Exports all contents of the given MCRDirectory to the
   * local filesystem, including all subdirectories and stored
   * files. If the local object is a file, the parent directory
   * of that file will be used for exporting.
   *
   * @param local the local directory where to export the contents to
   * @param dir the directory thats contents should be exported
   **/  
  public static void exportFiles( MCRDirectory dir, File local )
    throws MCRException
  {
    MCRArgumentChecker.ensureNotNull( dir,   "internal directory" );
    MCRArgumentChecker.ensureNotNull( local, "local file"         );
    
    String path = local.getPath();
    MCRArgumentChecker.ensureIsTrue( local.canWrite(), "Not writeable: " + path );
    
    // If local is file, use its parent instead
    if( local.isFile() ) local = local.getParentFile();
    
    MCRFilesystemNode[] children = dir.getChildren();
    for( int i = 0; i < children.length; i++ )
    {
      if( children[ i ] instanceof MCRFile )
      {
        MCRFile internalFile = (MCRFile)( children[ i ] );
        String name = internalFile.getName();
        
        File localFile = new File( local, name );
        try {
          internalFile.getContentTo( localFile ); }
        catch ( Exception ex ) {
          throw new MCRException("Can't get file content.",ex); }
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
