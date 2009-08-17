/*
 * $RCSfile$
 * $Revision: 15646 $ $Date: 2009-07-28 11:32:04 +0200 (Di, 28. Jul 2009) $
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
package org.mycore.plugins.depcollector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;

/**
 * Goal which adds dependencies of embedded artifacts.
 * 
 * ${basedir}/pom.xml is updated if dependencies has changed.
 *
 * @goal add-dependencies
 * 
 * @phase process-classes
 * @author Thomas Scheffler (yagee)
 */
public class MCRDependencyCollector extends AbstractMojo {
    /**
     * Location of the file.
     * @parameter expression="${basedir}"
     * @required
     */
    private File baseDir;

    /**
     * included pom file directory.
     * @parameter expression="${project.build.outputDirectory}/META-INF/maven"
     * @required
     */
    private File pomBaseDirectory;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder(false);

    private static final Namespace POM_NS = Namespace.getNamespace("", "http://maven.apache.org/POM/4.0.0");

    private static final String EXCLUDE_GROUPID = "org.mycore";

    private static final String EXCLUDE_MODULE = "mycore-complete";

    public void execute() throws MojoExecutionException {
        File mainPom = new File(baseDir, "pom.xml");
        List<File> pomFiles = getPomFiles(pomBaseDirectory);
        TreeMap<Artifact, Element> dependencies = new TreeMap<Artifact, Element>();
        try {
            for (File pomFile : pomFiles) {
                dependencies.putAll(getDependencies(pomFile));
            }
            List<Content> dependencyList = new ArrayList<Content>();
            String abstractName = mainPom.getParentFile().getName() + "/pom.xml";
            dependencyList.add(new Comment("BEGIN of generated dependencies of " + abstractName));
            Format outputFormat = Format.getPrettyFormat();
            outputFormat.setTextMode(TextMode.TRIM_FULL_WHITE);
            XMLOutputter xout = new XMLOutputter(outputFormat);
            for (Map.Entry<Artifact, Element> artifact : dependencies.entrySet()) {
                dependencyList.add(artifact.getValue().detach());
            }
            dependencyList.add(new Comment("END of generated dependencies of " + abstractName));
            Document pomDoc = SAX_BUILDER.build(mainPom);
            Map<Artifact, Element> oldDependencies = removeGeneratedDependencies(pomDoc);
            Element newDependencies = pomDoc.getRootElement().getChild("dependencies", POM_NS);
            newDependencies.addContent(dependencyList);
            if (!isEqual(oldDependencies, dependencies)) {
                getLog().info("Generating new " + mainPom);
                FileOutputStream fout = new FileOutputStream(mainPom);
                try {
                    xout.output(pomDoc, fout);
                } finally {
                    fout.close();
                }
            } else {
                getLog().info("Dependencies are not changed.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not collect dependencies.", e);
        }
    }

    private static Map<? extends Artifact, ? extends Element> getDependencies(File pomFile) throws JDOMException, IOException {
        Document pomDoc = SAX_BUILDER.build(pomFile);
        Element modules = pomDoc.getRootElement().getChild("modules", POM_NS);
        TreeMap<Artifact, Element> dependencies = new TreeMap<Artifact, Element>();
        if (modules != null) {
            @SuppressWarnings("unchecked")
            List<Element> children = modules.getChildren();
            for (Element module : children) {
                File moduleDir = new File(pomFile.getParentFile(), module.getTextTrim());
                if (moduleDir.getName().equals(EXCLUDE_MODULE))
                    continue;
                File modulePom = new File(moduleDir, "pom.xml");
                dependencies.putAll(getDependencies(modulePom));
            }
        } else {
            //fetch dependencies
            Element dependenciesElement = pomDoc.getRootElement().getChild("dependencies", POM_NS);
            if (dependenciesElement == null) {
                String packaging = pomDoc.getRootElement().getChildTextTrim("packaging", POM_NS);
                if (packaging == null || !packaging.equals("pom"))
                    System.err.println("WARNING: Could not find any dependencies in " + pomFile.getAbsolutePath());
                return Collections.emptyMap();
            }
            @SuppressWarnings("unchecked")
            List<Element> dependenciesList = dependenciesElement.getChildren("dependency", POM_NS);
            for (Element dependency : dependenciesList) {
                Artifact artifact = getArtifact(dependency);
                if (artifact.getGroupId().equals(EXCLUDE_GROUPID))
                    continue;
                dependencies.put(artifact, dependency);
            }
        }
        return dependencies;
    }

    private static Artifact getArtifact(Element dependency) {
        String groupId = dependency.getChildText("groupId", POM_NS);
        String artifactId = dependency.getChildText("artifactId", POM_NS);
        Artifact artifact = new Artifact(groupId, artifactId);
        return artifact;
    }

    private List<File> getPomFiles(File directory) {
        getLog().debug("Looking in directory: " + directory);
        if (!directory.isDirectory())
            return Collections.emptyList();
        ArrayList<File> pomFiles = new ArrayList<File>();
        for (File child : directory.listFiles()) {
            if (child.isDirectory())
                pomFiles.addAll(getPomFiles(child));
            else if (child.getName().equals("pom.xml")) {
                getLog().debug("Adding dependencies of " + child);
                pomFiles.add(child);
            }
        }
        return pomFiles;
    }

    private Map<Artifact, Element> removeGeneratedDependencies(Document pomDoc) throws JDOMException, IOException {
        Element dependenciesElement = pomDoc.getRootElement().getChild("dependencies", POM_NS);
        @SuppressWarnings("unchecked")
        Iterator<Content> contents = dependenciesElement.getContent().iterator();
        boolean removableDependency = false;
        TreeMap<Artifact, Element> dependencies = new TreeMap<Artifact, Element>();
        while (contents.hasNext()) {
            Content content = contents.next();
            if (content instanceof Comment) {
                if (content.getValue().startsWith("BEGIN of"))
                    removableDependency = true;
                if (content.getValue().startsWith("END of")) {
                    removableDependency = false;
                    contents.remove();
                }
            }
            if (removableDependency) {
                contents.remove();
                if (content instanceof Element) {
                    Element dependency = (Element) content;
                    dependencies.put(getArtifact(dependency), dependency);
                }
            }
        }
        return dependencies;
    }

    private boolean isEqual(Map<Artifact, Element> dep1, Map<Artifact, Element> dep2) {
        if (dep1 == dep2)
            return true;
        if (dep1 == null || dep2 == null)
            return false;
        if (dep1.size() != dep2.size())
            return false;
        //same size, same artifacts?
        TreeMap<Artifact, Element> dependencies = new TreeMap<Artifact, Element>();
        dependencies.putAll(dep1);
        dependencies.putAll(dep2);
        return (dependencies.size() == dep1.size());
    }
}
