/**
 * $RCSfile: MCRXMLSchemaTask.java,v $
 * $Revision: 1.0 $ $Date: 24.06.2008 12:21:55 $
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

public class MCRGetMyCoReJarTask extends Task {

    private Path classPath;

    private boolean scanEveryJarFile = false;

    private File mycoreJarFile;
    
    private String property;

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

    @Override
    public void execute() throws BuildException {
        if (property==null){
            throw new BuildException("No property attribute specified");
        }
        Exception ex = null;
        try {
            findMycoreJarInPath(classPath);
        } catch (IOException e) {
            ex = e;
        }
        if (mycoreJarFile == null) {
            throw new BuildException("Could not find a valid mycore.jar in classPath.", ex);
        }
        getProject().setProperty(property, mycoreJarFile.getAbsolutePath());
    }

    private void findMycoreJarInPath(Path path) throws IOException {
        for (String part : path.list()) {
            log("Checking pathElement:" + part, Project.MSG_DEBUG);
            File candidate = new File(part);
            if (scanEveryJarFile || candidate.getName().startsWith("mycore")) {
                if (isMyCoReJAR(candidate)) {
                    log("Found mycore in " + candidate.getAbsolutePath());
                    break;
                }
            }
        }
        if (this.mycoreJarFile == null && !scanEveryJarFile) {
            log("Did not found a mycore jar file starting with 'mycore' in classPath. Now scanning every jar file for a MyCoRe manifest.");
            scanEveryJarFile = true;
            findMycoreJarInPath(path);
        }
    }

    private boolean isMyCoReJAR(File candidate) throws IOException {
        JarFile mycoreJar = new JarFile(candidate);
        Manifest manifest = mycoreJar.getManifest();
        if (manifest == null || manifest.getMainAttributes() == null)
            return false;
        // Assume it's a mycore jar file if 'MCR-Version' attribute is present
        // in jar file
        final String mcrVersion = manifest.getMainAttributes().getValue("MCR-Version");
        if (mcrVersion != null) {
            this.mycoreJarFile = candidate;
            return true;
        }
        return false;
    }

    public void setProperty(String property) {
        this.property = property;
    }

}
