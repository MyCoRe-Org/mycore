/**
 * $RCSfile: MCRIntegrateTask.java,v $
 * $Revision: 1.0 $ $Date: 18.06.2008 09:40:38 $
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.SubAnt;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;

/**
 * Calls a given target of the integrate.xml inside a MyCoRe jar file.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRIntegrateTask extends Task {

    private Path classPath;

    private String mcrVersion, target;

    private File buildDir, mycoreJarFile = null;

    private JarFile mycoreJar;

    private boolean scanEveryJarFile = false;

    @Override
    public void execute() {
        log("classPath:" + classPath, Project.MSG_DEBUG);
        Exception ex = null;
        try {
            findMycoreJarInPath(classPath);
        } catch (IOException e) {
            ex = e;
        }
        if (mycoreJar == null) {
            throw new BuildException("Could not find a valid mycore.jar in classPath.", ex);
        }
        extractComponents();
        callSubAnt();
        super.execute();
    }

    private void callSubAnt() {
        if (target == null)
            throw new BuildException("Cannot integrate MyCoRe components. No 'target' definied.");
        SubAnt subAnt = new SubAnt();
        subAnt.bindToOwner(this);
        subAnt.setBuildpath(new Path(getProject(), buildDir.getAbsolutePath()));
        subAnt.setAntfile("integrate.xml");
        subAnt.setTarget(target);
        subAnt.execute();
    }

    private void extractComponents() {
        Expand expandTask = new Expand();
        expandTask.bindToOwner(this);
        if (buildDir == null) {
            buildDir = getUnpackDir();
        }
        if (!buildDir.exists()) {
            buildDir.mkdirs();
        }
        expandTask.setDest(buildDir);
        expandTask.setSrc(mycoreJarFile);
        PatternSet expandSet = new PatternSet();
        expandSet.setProject(getProject());
        expandSet.setIncludes("integrate.xml components/**");
        for (String excluded : getExcludedComponents()) {
            expandSet.setExcludes(new StringBuilder("components/").append(excluded).append("/**").toString());
        }
        expandTask.addPatternset(expandSet);
        expandTask.execute();
    }

    private File getUnpackDir() {
        File buildDir = new File(getProject().getBaseDir(), "build");
        return new File(buildDir, "components");
    }

    public void setClassPathRef(Reference ref) {
        if (classPath == null) {
            classPath = new Path(getProject());
        }
        classPath.createPath().setRefid(ref);
    }

    private void findMycoreJarInPath(Path path) throws IOException {
        for (String part : path.list()) {
            log("Checking pathElement:" + part, Project.MSG_DEBUG);
            File candidate = new File(part);
            if (scanEveryJarFile || candidate.getName().startsWith("mycore")) {
                if (isMyCoReJAR(candidate)) {
                    log("Found mycore " + mcrVersion + " in " + candidate.getAbsolutePath());
                    break;
                }
            }
        }
        if (this.mycoreJar == null) {
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
            this.mcrVersion = mcrVersion;
            this.mycoreJar = mycoreJar;
            this.mycoreJarFile = candidate;
            return true;
        }
        return false;
    }

    private Set<String> getExcludedComponents() {
        String excludedValue = getProject().getProperty("MCR.Components.Exclude");
        if (excludedValue == null)
            return Collections.emptySet();
        log("Excluding " + excludedValue + " from integration.");
        HashSet<String> excludedComponents = new HashSet<String>();
        String[] excludedValues = excludedValue.split(",");
        for (String component : excludedValues) {
            excludedComponents.add(component.trim());
        }
        return excludedComponents;
    }

    public void setTarget(String target) {
        this.target = target;
    }

}
