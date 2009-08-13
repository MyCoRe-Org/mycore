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
package org.mycore.buildtools.maventools;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDependencyCollector {
    private static final SAXBuilder SAX_BUILDER = new SAXBuilder(false);

    private static final Namespace POM_NS = Namespace.getNamespace("", "http://maven.apache.org/POM/4.0.0");

    private static final String EXCLUDE_GROUPID = "org.mycore";

    private static final String EXCLUDE_MODULE = "mycore-complete";

    /**
     * Prints all dependencies alphabetically sorted by artifactId from a pom file.
     * @param args
     * @throws IOException 
     * @throws JDOMException 
     */
    public static void main(String[] args) throws JDOMException, IOException {
        if (args.length < 1) {
            System.err.println("Usage MCRDependencyCollector {pomfile}");
            System.exit(1);
        }
        File mainPom = new File(args[0]);
        if (!mainPom.isFile()) {
            System.err.println("Cannot find pom file: " + mainPom.getAbsolutePath());
            System.err.println("Usage MCRDependencyCollector {pomfile}");
            System.exit(1);
        }
        TreeMap<Artifact, Element> dependencies = new TreeMap<Artifact, Element>();
        dependencies.putAll(getDependencies(mainPom));
        Element dependenciesElement = new Element("dependencies", POM_NS);
        String abstractName = mainPom.getParentFile().getName() + "/pom.xml";
        dependenciesElement.addContent(new Comment("BEGIN of dependencies of " + abstractName));
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        for (Map.Entry<Artifact, Element> artifact : dependencies.entrySet()) {
            dependenciesElement.addContent(artifact.getValue().detach());
        }
        dependenciesElement.addContent(new Comment("END of dependencies of " + abstractName));
        xout.output(dependenciesElement, System.out);
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
}
