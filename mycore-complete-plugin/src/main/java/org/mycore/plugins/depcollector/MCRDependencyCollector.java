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

/**
 * Goal which adds dependencies of embedded artifacts.
 *
 * @goal add-dependencies
 * 
 * @phase process-sources
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
            List<Content> dependencyList=new ArrayList<Content>();
            String abstractName = mainPom.getParentFile().getName() + "/pom.xml";
            dependencyList.add(new Comment("BEGIN of generated dependencies of " + abstractName));
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            for (Map.Entry<Artifact, Element> artifact : dependencies.entrySet()) {
                dependencyList.add(artifact.getValue().detach());
            }
            dependencyList.add(new Comment("END of generated dependencies of " + abstractName));
            Document newPom = removeGeneratedDependencies(mainPom);
            Element newDependencies = newPom.getRootElement().getChild("dependencies", POM_NS);
            newDependencies.addContent(dependencyList);
            xout.output(newPom, System.out);
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
                String groupId = dependency.getChildText("groupId", POM_NS);
                if (groupId.equals(EXCLUDE_GROUPID))
                    continue;
                String artifactId = dependency.getChildText("artifactId", POM_NS);
                Artifact artifact = new Artifact(groupId, artifactId);
                dependencies.put(artifact, dependency);
            }
        }
        return dependencies;
    }

    private List<File> getPomFiles(File directory) {
        getLog().info("Looking in directory: " + directory);
        if (!directory.isDirectory())
            return Collections.emptyList();
        ArrayList<File> pomFiles = new ArrayList<File>();
        for (File child : directory.listFiles()) {
            if (child.isDirectory())
                pomFiles.addAll(getPomFiles(child));
            else if (child.getName().equals("pom.xml"))
                pomFiles.add(child);
        }
        return pomFiles;
    }

    private Document removeGeneratedDependencies(File mainPom) throws JDOMException, IOException {
        Document pomDoc = SAX_BUILDER.build(mainPom);
        Element dependenciesElement = pomDoc.getRootElement().getChild("dependencies", POM_NS);
        @SuppressWarnings("unchecked")
        Iterator<Content> contents = dependenciesElement.getContent().iterator();
        boolean removableDependency = false;
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
            if (removableDependency)
                contents.remove();
        }
        return pomDoc;
    }
}
