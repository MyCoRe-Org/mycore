/**
 * $RCSfile: MCRXMLSchemaTask.java,v $
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
package org.mycore.buildtools.anttasks;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * A subclass to call a given target of the integrate.xml inside a jar file.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 */
public class MCRGetJarTask extends Task {

    private Path classPath;

    private boolean scanEveryJarFile = false;

    private File jarFile;

    private String property;

    private String jarStartsWith;

    @Override
    public void init() throws BuildException {
        super.init();
    }

    public void setClassPathRef(Reference ref) {
        if (classPath == null) {
            classPath = new Path(getProject());
        }
        classPath.createPath().setRefid(ref);
    }

    public String getJarStartsWith() {
        return jarStartsWith == null ? "mycore" : jarStartsWith;
    }

    public void setJarStartsWith(String jarStartsWith) {
        this.jarStartsWith = jarStartsWith;
    }

    @Override
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("No property attribute specified");
        }
        Exception ex = null;
        try {
            findJarInPath(classPath);
        } catch (IOException e) {
            ex = e;
        }
        if (jarFile == null) {
            throw new BuildException("Could not find a valid " + getJarStartsWith() + "*.jar in classPath.", ex);
        }
        getProject().setProperty(property, jarFile.getAbsolutePath());
    }

    private void findJarInPath(Path path) throws IOException {
        log("Checking path:" + path, Project.MSG_DEBUG);
        for (String part : path.list()) {
            log("Checking pathElement:" + part, Project.MSG_DEBUG);
            File candidate = new File(part);
            if (scanEveryJarFile || candidate.getName().startsWith(getJarStartsWith())) {
                log("Checking candidate:" + candidate, Project.MSG_DEBUG);
                if (isJAR(candidate)) {
                    log("Found " + getJarStartsWith() + " in " + candidate.getAbsolutePath());
                    break;
                }
            }
        }
        if (this.jarFile == null && !scanEveryJarFile) {
            log("Did not found a jar file starting with '" + getJarStartsWith()
                + "' in classPath. Now scanning every jar file for a manifest.");
            scanEveryJarFile = true;
            findJarInPath(path);
        }
    }

    private boolean isJAR(File candidate) throws IOException {
        if (!candidate.isFile()) {
            return false;
        }
        JarFile jar = new JarFile(candidate);
        Manifest manifest = jar.getManifest();
        jar.close();
        if (manifest == null || manifest.getMainAttributes() == null)
            return false;
        // Assume it's a mycore jar file if 'MCR-Version' attribute is present
        // in jar file
        final String mcrVersion = manifest.getMainAttributes().getValue("MCR-Version");
        if (mcrVersion != null) {
            this.jarFile = candidate;
            return true;
        }
        return false;
    }

    public void setProperty(String property) {
        this.property = property;
    }

}
