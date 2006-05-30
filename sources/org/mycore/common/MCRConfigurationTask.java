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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task that allows 'mycore.properties' manipulation via ant.
 * @author Thomas Scheffler (yagee)
 */
public class MCRConfigurationTask extends Task {
    // some constants
    private static final String MCR_CONFIGURATION_INCLUDE = "MCR.configuration.include";

    private static final Pattern INCLUDE_PATTERN = Pattern.compile(MCR_CONFIGURATION_INCLUDE);

    // some fields settable and gettable via methods
    private String action;

    private String value;

    private File propertyFile;

    // some fields needed for processing
    private boolean valuePresent = false;

    private boolean propertiesLoaded = false;

    private boolean fileChanged = false;

    private int lineNumber = -1;

    private ArrayList propLines;

    /**
     * Execute the requested operation.
     * 
     * @throws BuildException
     *             if an error occurs
     */
    public void execute() throws BuildException {
        checkPreConditions();
        loadLines();
        if (!propertiesLoaded) {
            throw new BuildException("Could not load: " + propertyFile.getName());
        }
        if (action.equals("addInclude")) {
            addInclude();
        } else if (action.equals("removeInclude")) {
            removeInclude();
        }
        if (fileChanged) {
            writeLines();
        }
        reset();
    }

    /**
     * checks whether all preconditions are met
     */
    private void checkPreConditions() throws BuildException {
        if (action == null) {
            throw new BuildException("Must specify 'action' attribute");
        }
        if (propertyFile == null) {
            throw new BuildException("Must specify 'propertyfile' attribute");
        }
        if (value == null) {
            throw new BuildException("Must specify 'value' attribute");
        }
        if (!action.equals("addInclude") && !action.equals("removeInclude")) {
            throw new BuildException("action must be either 'addInclude' or 'removeInclude'");
        }
        if (!propertyFile.exists()) {
            throw new BuildException(new FileNotFoundException(propertyFile.getName() + " does not exist."));
        }
    }

    /**
     * resets all local fields
     */
    private void reset() {
        action = null;
        propertyFile = null;
        lineNumber = -1;
        propertiesLoaded = false;
        fileChanged = false;
        valuePresent = false;
    }

    /**
     * adds an include
     */
    private void addInclude() {
        if (valuePresent) {
            handleOutput(new StringBuffer("Not changing ").append(propertyFile.getName()).append(": '").append(value).append("' allready included.").toString());
            return;
        }
        fileChanged = true;
        String prop = propLines.get(lineNumber).toString();
        String newProp = prop + "," + value;
        propLines.remove(lineNumber);
        propLines.add(lineNumber, newProp);
        handleOutput(new StringBuffer(propertyFile.getName()).append(':').append(lineNumber).append(" added '").append(value).append("' to ").append(
                MCR_CONFIGURATION_INCLUDE).toString());
    }

    /**
     * removes an include
     */
    private void removeInclude() {
        if (!valuePresent) {
            handleOutput(new StringBuffer("Not changing ").append(propertyFile.getName()).append(": '").append(value).append("' not present.").toString());
            return;
        }
        fileChanged = true;
        String newProp = propLines.get(lineNumber).toString().replaceAll("," + value, "");
        propLines.remove(lineNumber);
        propLines.add(lineNumber, newProp);
        handleOutput(new StringBuffer(propertyFile.getName()).append(':').append(lineNumber).append(" removed '").append(value).append("' from ").append(
                MCR_CONFIGURATION_INCLUDE).toString());
    }

    /*
     * writes back the property file together with changed properties
     */
    private void writeLines() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertyFile), Charset.forName("ISO-8859-1")));
            for (Iterator it = propLines.iterator(); it.hasNext();) {
                writer.write(it.next().toString());
                writer.newLine();
            }
        } catch (IOException e) {
            handleErrorOutput("Error while writing '" + propertyFile.getName() + "': " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    handleErrorOutput("Error while closing file '" + propertyFile.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    /*
     * loads the property file and marks the occurence of
     * MCR.configuration.include
     */
    private void loadLines() {
        BufferedReader reader = null;
        propertiesLoaded = false;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFile), Charset.forName("ISO-8859-1")));
            propLines = new ArrayList(1000);
            int i = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                // add each line of the property file to the array list
                propLines.add(line);
                if ((lineNumber < 0) && INCLUDE_PATTERN.matcher(line).find()) {
                    // found the MCR.configuration.include line
                    lineNumber = i;
                    if (line.indexOf(value) > 0) {
                        // value is included
                        valuePresent = true;
                    }
                }
                i++;
            }
            propertiesLoaded = true;
        } catch (IOException e) {
            handleErrorOutput("Error while reading '" + propertyFile.getName() + "': " + e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                handleErrorOutput("Error while closing file '" + propertyFile.getName() + "': " + e.getMessage());
            }
        }
    }

    public String getAction() {
        return action;
    }

    /**
     * sets the action the task should perform.
     * 
     * @param action
     *            either "addInclude" or "removeInclude"
     */
    public void setAction(String action) {
        this.action = action;
    }

    public File getPropertyFile() {
        return propertyFile;
    }

    /**
     * sets the property file that needs to be changed.
     * 
     * @param action
     *            a 'mycore.properties' file
     */
    public void setPropertyFile(File propertyFile) {
        this.propertyFile = propertyFile;
    }

    public String getValue() {
        return value;
    }

    /**
     * sets the value for the action to be performed. For 'addInclude' a value
     * of "mycore.properties.moduleXY" would result in adding
     * ",mycore.properties.moduleXY" to the property
     * "MCR.configuration.include".
     * 
     * @param action
     *            a 'mycore.properties' file
     */
    public void setValue(String value) {
        this.value = value;
    }
}