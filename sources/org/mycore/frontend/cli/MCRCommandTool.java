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

package mycore.commandline;

import java.io.*;
import mycore.common.*;
import mycore.datamodel.*;

/**
 * This class implements a main program, which can used for working 
 * with a MyCoRe application data at commandline.
 * <p>
 * The programm is a working solution for tests. It should replace
 * with a better program later.
 * <p>
 * 
 * The follwing actions you can do:
 * - load an object to the datastore
 * - delete an object from the datastore
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRCommandTool
{

private static String SLASH = new String((System.getProperties())
  .getProperty("file.separator"));

/**
 * The program for handling data with the command line.
 *
 * @param args   the argument string from the command line.
 * 
 * Call:
 * to load   - java MCRCommandTool load [dir|file]
 * to delete - java MCRCommandTool delete object_id
 * to show   - java MCRCommandTool show object_id
 * to update - java MCRCommandTool load [dir|file]
 * to query  - java MCRCommandTool query type query
 **/
public static void main(String[] args)
  {
  int todo = 0;

  System.out.println();
  System.out.println("* * * M y C o R e * * *");
  System.out.println();
  System.out.println("Version 1.0");
  System.out.println();

  if(args[0] == null) { 
    System.out.println("Error, missing the 1th argument.");
    System.out.println(); 
    System.exit(1); }
  args[0] = args[0].toLowerCase();
  System.out.print("The arguments of call are ");
  for (int i=0;i<args.length;i++)
    { System.out.print(args[i]+" "); }
  System.out.println();
  if (args[0].equals("delete")) { todo = 1; }
  if (args[0].equals("load")) { todo = 2; }
  if (args[0].equals("show")) { todo = 3; }
  if (args[0].equals("update")) { todo = 4; }
  if (args[0].equals("query")) { todo = 5; }
  switch(todo) {
    case 1: delete(args); break;
    case 2: load(args); break;
    case 3: show(args); break;
    case 4: update(args); break;
    case 5: query(args); break;
    default : usage(); System.exit(1);
    }
  System.out.println("Ready.");
  System.out.println();
  System.exit(0);
  }

/**
 * This methode print the usage of this tool.
 **/
private static final void usage()
  {
  System.out.println(); 
  System.out.println
    ("Usage : java MCRCommandTool load [dir|file]");
  System.out.println
    ("        java MCRCommandTool delete object_id");
  System.out.println
    ("        java MCRCommandTool show object_id");
  System.out.println
    ("        java MCRCommandTool update [dir|file]");
  System.out.println
    ("        java MCRCommandTool query type query");
  System.out.println(); 
  }

/**
 * This methode delete a single object from the persistece datastore
 * for this MCRObject.
 * 
 * @param args   the argument stack from the command line
 **/
private static final void delete(String[] args)
  {
  if(args[1] == null) { 
    System.out.println("Error, missing the 2nd argument.");
    usage();
    System.exit(1); }
  try {
    MCRObject mycore_obj = new MCRObject();
    mycore_obj.deleteFromDatastore(args[1]);
    System.out.println(mycore_obj.getId().getId()+" deleted.\n"); }
  catch (Exception e) {
    System.out.println("\n"+e.getMessage());
    System.out.println("Object "+args[1]+" ignored.\n"); System.exit(1); }
  }

/**
 * This methode load a single object or objects in a directory
 * in the persistece datastore for this MCRObject.
 * 
 * @param args   the argument stack from the command line
 **/
private static final void load(String[] args)
  {
  MCRObject mycore_obj = new MCRObject();
  int inarg_len;
  if(args[1] == null) { 
    System.out.println("Error, missing the 2nd argument.");
    usage();
    System.exit(1); }
  File inarg = new File (args[1]);
  if (inarg.isDirectory()) {
    String [] inarg_list = inarg.list();
    if (inarg_list.length == 0) {
      System.out.println("No file was found in the directory.");
      System.out.println();
      System.exit(1); }
    int dircount = 0;
    for (int i=0;i<inarg_list.length;i++) {
      inarg_len = inarg_list[i].length();
      if (inarg_len<5) { continue; }
      if (!inarg_list[i].substring(inarg_len-4,inarg_len).equals(".xml"))
        { continue; }
      System.out.println("Reading file "+inarg_list[i]+" ...\n");
      try {
        String uri = args[1]+SLASH+inarg_list[i];
        mycore_obj = new MCRObject();
        mycore_obj.setFromURI(uri);
        System.out.println("Label --> "+mycore_obj.getLabel());
        mycore_obj.createInDatastore();
        }
      catch (Exception e)
        { System.out.println(e.getMessage()); 
          System.out.println("File ignored.\n"); continue; }
      System.out.println(mycore_obj.getId().getId()+" loaded.\n");
      dircount++;
      }
    if (dircount == 0) {
      System.out.println(
        "No valid *.xml file was found in the directory.");
      System.out.println();
      System.exit(1); }
    System.exit(0); }
  if (args[1].length()<5) {
    System.out.println("The file name is false.");
    System.out.println();
    System.exit(1); }
  if (inarg.isFile() &&
    args[1].substring(args[1].length()-4,args[1].length()).equals(".xml")) {
    System.out.println("Reading file "+args[1]+" ...\n");
    try {
      String uri = args[1];
      mycore_obj.setFromURI(uri);
      System.out.println("Label --> "+mycore_obj.getLabel());
      mycore_obj.createInDatastore(); 
      }
    catch (Exception e)
      { System.out.println(e.getMessage());
        System.out.println("File ignored.\n"); System.exit(1); }
    System.out.println(mycore_obj.getId().getId()+" loaded.\n");
    }
  else {
    System.out.println("No valid *.xml file was found.");
    System.out.println();
    System.exit(1); }
  }

/**
 * This methode shows a single object from the persistece datastore
 * for this MCRObject.
 * 
 * @param args   the argument stack from the command line
 **/
private static final void show(String[] args)
  {
  if(args[1] == null) { 
    System.out.println("Error, missing the 2nd argument.");
    usage();
    System.exit(1); }
  try {
    MCRObject mycore_obj = new MCRObject();
    mycore_obj.receiveFromDatastore(args[1]);
    mycore_obj.debug();
    MCRObjectStructure st = mycore_obj.getStructure();
    if (st != null) { st.debug(); }
    MCRMetaLangText me = 
      (MCRMetaLangText)mycore_obj.getMetadataElement("titles");
    if (me != null) {me.debug(); }
    MCRObjectService se = mycore_obj.getService();
    if (se != null) { se.debug(); }
    }
  catch (Exception e) {
    System.out.println("\n"+e.getMessage());
    System.out.println("Object "+args[1]+" ignored.\n"); System.exit(1); }
  }

/**
 * This methode update a single object or objects in a directory
 * in the persistece datastore for this MCRObject.
 * 
 * @param args   the argument stack from the command line
 **/
private static final void update(String[] args)
  {
  MCRObject mycore_obj = new MCRObject();
  int inarg_len;
  if(args[1] == null) { 
    System.out.println("Error, missing the 2nd argument.");
    usage();
    System.exit(1); }
  File inarg = new File (args[1]);
  if (inarg.isDirectory()) {
    String [] inarg_list = inarg.list();
    if (inarg_list.length == 0) {
      System.out.println("No file was found in the directory.");
      System.out.println();
      System.exit(1); }
    int dircount = 0;
    for (int i=0;i<inarg_list.length;i++) {
      inarg_len = inarg_list[i].length();
      if (inarg_len<5) { continue; }
      if (!inarg_list[i].substring(inarg_len-4,inarg_len).equals(".xml"))
        { continue; }
      System.out.println("Reading file "+inarg_list[i]+" ...\n");
      try {
        String uri = args[1]+SLASH+inarg_list[i];
        mycore_obj = new MCRObject();
        mycore_obj.setFromURI(uri);
        System.out.println("Label --> "+mycore_obj.getLabel());
        mycore_obj.updateInDatastore();
        }
      catch (Exception e)
        { System.out.println(e.getMessage()); 
          System.out.println("File ignored.\n"); continue; }
      System.out.println(mycore_obj.getId().getId()+" updated.\n");
      dircount++;
      }
    if (dircount == 0) {
      System.out.println(
        "No valid *.xml file was found in the directory.");
      System.out.println();
      System.exit(1); }
    System.exit(0); }
  if (args[1].length()<5) {
    System.out.println("The file name is false.");
    System.out.println();
    System.exit(1); }
  if (inarg.isFile() &&
    args[1].substring(args[1].length()-4,args[1].length()).equals(".xml")) {
    System.out.println("Reading file "+args[1]+" ...\n");
    try {
      String uri = args[1];
      mycore_obj.setFromURI(uri);
      System.out.println("Label --> "+mycore_obj.getLabel());
      mycore_obj.updateInDatastore(); 
      }
    catch (Exception e)
      { System.out.println(e.getMessage());
        System.out.println("File ignored.\n"); System.exit(1); }
    System.out.println(mycore_obj.getId().getId()+" updated.\n");
    }
  else {
    System.out.println("No valid *.xml file was found.");
    System.out.println();
    System.exit(1); }
  }

/**
 * This methode shows a list of MCRObjectID's as result of the query.
 * 
 * @param args   the argument stack from the command line
 **/
private static final void query(String[] args)
  {
  if(args[1] == null) { 
    System.out.println("Error, missing the 2nd argument (type).");
    usage();
    System.exit(1); }
  if(args[2] == null) { 
    System.out.println("Error, missing the 3nd argument (query string).");
    usage();
    System.exit(1); }
  try {
    String type = args[1];
    String query = args[2];
    MCRQueryResult mycore_obj = new MCRQueryResult(type,query);
    mycore_obj.debug();
    }
  catch (Exception e) {
    System.out.println("\n"+e.getMessage());
    System.out.println("Query for "+args[1]+" ignored.\n"); System.exit(1); }
  }

}

