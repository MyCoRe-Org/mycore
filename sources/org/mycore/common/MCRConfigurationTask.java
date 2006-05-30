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
package org.mycore.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.tools.ant.Task;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class MCRConfigurationTask extends Task {
    String action;
    String value;
    File propertyFile;
    Pattern INCLUDE_PATTERN= Pattern.compile("MCR.configuration.include");
    boolean valuePresent=false;
    ArrayList propLines;
    int lineNumber;
    boolean propertiesLoaded=false;
    boolean fileChanged=false;
    
    public void execute(){
        if (!action.equals("addInclude") && !action.equals("removeInclude")){
            throw new IllegalArgumentException("action must be either 'addInclude' or 'removeInclude'");
        }
        if (!propertyFile.exists()){
            throw new RuntimeException(new FileNotFoundException(propertyFile.getName()+" does not exist."));
        }
        loadLines();
        if (!propertiesLoaded){
            throw new RuntimeException("Could not load: "+propertyFile.getName());
        }
        if (action.equals("addInclude")){
            addInclude();
        } else if (action.equals("removeInclude")){
            removeInclude();
        }
        if (fileChanged){
            writeLines();
        }
        action=null;propertyFile=null;lineNumber=-1;propertiesLoaded=false;fileChanged=false;valuePresent=false;
        
    }
    
    private void addInclude(){
        if (valuePresent){
            return;
        }
        fileChanged=true;
        String prop=propLines.get(lineNumber).toString();
        String newProp=prop+","+value;
        propLines.remove(lineNumber);
        propLines.add(lineNumber,newProp);
    }
    
    private void removeInclude(){
        if (valuePresent){
            return;
        }
        fileChanged=true;
        String newProp=propLines.get(lineNumber).toString().replaceAll(","+value,"");
        propLines.remove(lineNumber);
        propLines.add(lineNumber,newProp);
    }
    
    private void writeLines() {
        BufferedWriter writer=null;
        try {
            writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertyFile), Charset.forName("ISO-8859-1")));
            for (Iterator it=propLines.iterator();it.hasNext();){
                writer.write(it.next().toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer!=null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void loadLines() {
        BufferedReader reader = null;
        propertiesLoaded = false;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFile), Charset.forName("ISO-8859-1")));
            propLines = new ArrayList(1000);
            int i = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                propLines.add(line);// add each line of the property file to the
                                    // array list
                if (INCLUDE_PATTERN.matcher(line).find()) {
                    lineNumber = i;
                    if (line.indexOf(value) > 0) {
                        valuePresent = true;
                    }
                }
                i++;
            }
            propertiesLoaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public File getPropertyFile() {
        return propertyFile;
    }
    public void setPropertyFile(File propertyFile) {
        this.propertyFile = propertyFile;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

}
