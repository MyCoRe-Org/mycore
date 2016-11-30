/**
 * $RCSfile: MCRIntegrateTask.java,v $ $Revision$ $Date$ This file is part of ** M y C o R e
 * ** Visit our homepage at http://www.mycore.de/ for details. This program is free software; you can use it,
 * redistribute it and / or modify it under the terms of the GNU General Public License (GPL) as published by the Free
 * Software Foundation; either version 2 of the License or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should
 * have received a copy of the GNU General Public License along with this program, normally in the file license.txt. If
 * not, write to the Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 **/
package org.mycore.buildtools.anttasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.SubAnt;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Calls a given target of the integrate.xml inside a jar file.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 */
public class MCRIntegrateTask extends Task {

    private Path classPath;

    private String target;

    private String jarStartsWith;

    private File buildDir, jarFile = null;

    private JarFile mycoreJar;

    private DocumentBuilder docBuilder;

    private TransformerFactory transformerFactory;

    private Reference classPathRef;

    @Override
    public void init() {
        super.init();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        transformerFactory = TransformerFactory.newInstance();
        try {
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new BuildException("Can not instanciate DocumentBuilder");
        }
    }

    @Override
    public void execute() {
        log("classPath:" + classPath, Project.MSG_DEBUG);
        try {
            getJar();
        } catch (IOException e) {
            throw new BuildException("Cannot find a JAR file in classpath.", e);
        }
        extractComponents();
        try {
            writeIntegrationHelperFile();
        } catch (Exception e) {
            throw new BuildException("Cannot generate integration helper file.", e);
        }
        callSubAnt();
        super.execute();
    }

    public void setClassPathRef(Reference ref) {
        if (classPath == null) {
            classPath = new Path(getProject());
        }
        classPath.createPath().setRefid(ref);
        classPathRef = ref;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setBuildDir(String buildDir) {
        this.buildDir = new File(getProject().getBaseDir(), buildDir);
    }

    public String getJarStartsWith() {
        return jarStartsWith == null ? "mycore" : jarStartsWith;
    }

    public void setJarStartsWith(String jarStartsWith) {
        this.jarStartsWith = jarStartsWith;
    }

    private void getJar() throws IOException {
        if (jarFile == null) {
            Exception ex = null;
            try {
                String JAR_PROPERTY = "mcr.integrate." + getJarStartsWith() + ".jar";
                String pathName = getProject().getProperty(JAR_PROPERTY);
                if (pathName == null) {
                    MCRGetJarTask findTask = new MCRGetJarTask();
                    findTask.bindToOwner(this);
                    findTask.setProperty(JAR_PROPERTY);
                    findTask.setClassPathRef(classPathRef);
                    findTask.setJarStartsWith(getJarStartsWith());
                    findTask.execute();
                    pathName = getProject().getProperty(JAR_PROPERTY);
                }
                log("Found in " + pathName, Project.MSG_DEBUG);
                jarFile = new File(pathName);
            } catch (RuntimeException e) {
                e.printStackTrace();
                ex = e;
            }
            if (jarFile == null) {
                throw new BuildException("Could not find a valid " + getJarStartsWith() + ".jar in classPath.", ex);
            }
        }
        if (mycoreJar == null) {
            mycoreJar = new JarFile(jarFile);
        }
    }

    private void callSubAnt() {
        if (target == null)
            throw new BuildException("Cannot integrate components. No 'target' definied.");
        SubAnt subAnt = new SubAnt();
        subAnt.bindToOwner(this);
        subAnt.setBuildpath(new Path(getProject(), buildDir.getAbsolutePath()));
        subAnt.setAntfile("integrate.xml");
        final Property baseDir = new Property();
        baseDir.setLocation(buildDir);
        baseDir.setName("integration.dir");
        subAnt.addProperty(baseDir);
        subAnt.setTarget(target);
        subAnt.setInheritall(true);
        subAnt.setInheritrefs(true);
        subAnt.execute();
    }

    private void extractComponents() {
        Expand expandTask = new Expand();
        expandTask.bindToOwner(this);
        if (buildDir == null) {
            buildDir = getUnpackDir();
        }
        if (!buildDir.exists()) {
            if (!buildDir.mkdirs()) {
                throw new BuildException("Cannot create directory: " + buildDir.getAbsolutePath());
            }
        }
        expandTask.setDest(buildDir);
        expandTask.setSrc(jarFile);
        PatternSet expandSet = new PatternSet();
        expandSet.setProject(getProject());
        expandSet.setIncludes("integrate.xml config/** components/** web/**");
        for (String excluded : getExcludedComponents()) {
            expandSet.setExcludes("components/" + excluded + "/**");
        }
        expandTask.addPatternset(expandSet);
        expandTask.setOverwrite(false);
        expandTask.execute();
    }

    private File getUnpackDir() {
        if (this.buildDir == null) {
            File buildDir = new File(getProject().getBaseDir(), "build");
            return new File(buildDir, "components");
        } else {
            return this.buildDir;
        }
    }

    private Set<String> getExcludedComponents() {
        String excludedValue = getProject().getProperty("MCR.Components.Exclude");
        if (excludedValue == null || excludedValue.trim().length() == 0)
            return Collections.emptySet();
        log("Excluding " + excludedValue + " from integration.");
        return Arrays.stream(excludedValue.split(","))
                     .map(String::trim)
                     .collect(Collectors.toCollection(HashSet::new));
    }

    private void writeIntegrationHelperFile() throws IOException, TransformerConfigurationException,
        TransformerException {
        Document doc = docBuilder.newDocument();
        // create ant project file
        Element project = doc.createElement("project");
        project.setAttribute("name", "integrationhelper");
        doc.appendChild(project);
        // add integration.classpath definied by setClassPathRef()
        Element path = doc.createElement("path");
        path.setAttribute("id", "integration.classpath");
        for (String pathElement : classPath.list()) {
            Element child = doc.createElement("pathelement");
            child.setAttribute("location", pathElement);
            path.appendChild(child);
        }
        project.appendChild(path);
        // add property mycore.jar as a hint to mycore.jar file
        Element mycoreProperty = doc.createElement("property");
        mycoreProperty.setAttribute("name", getJarStartsWith() + ".jar");
        mycoreProperty.setAttribute("location", jarFile.getAbsolutePath());
        project.appendChild(mycoreProperty);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(buildDir, "helper.xml"));
            transformerFactory.newTransformer().transform(new DOMSource(doc), new StreamResult(out));
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

}
