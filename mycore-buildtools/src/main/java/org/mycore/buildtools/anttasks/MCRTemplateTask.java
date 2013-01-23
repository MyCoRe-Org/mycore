package org.mycore.buildtools.anttasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

/**
 * Ant task that is looking for new templates and automatically generating a new
 * chooseTemplate.xsl
 * 
 * @author Stephan Schmidt (mcrschmi)
 */

public class MCRTemplateTask extends Task {
    // filters the wrong directories
    private static class TemplateFilenameFilter implements FilenameFilter {

        public boolean accept(File f, String s) {
            return (s.contains("template"));
        }
    }

    // some fields settable and gettable via methods
    private String templatepath;

    private String choosepath;

    private static final Namespace XSL = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");

    /**
     * Execute the requested operation.
     * 
     * throws BuildException if an error occurs
     */
    public void execute() throws BuildException {
        File templatedir = new File(templatepath);
        String[] directories = templatedir.list(new TemplateFilenameFilter());
        log("Templates in directory: " + templatedir.getAbsolutePath());
        for (int i = 0; i < directories.length; i++) {
            log(i + ":" + directories[i]);
        }
        try {
            createxsl(directories);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * 
     * is creating the chooseTemplate.xsl
     * @throws IOException 
     * 
     */
    public void createxsl(String[] directory) throws IOException {
        String temppath = "";
        String temptest = "";

        Element rootOut = new Element("stylesheet", XSL).setAttribute("version", "1.0");
        Document jdom = new Document(rootOut);

        for (String aDirectory1 : directory) {
            temppath = aDirectory1 + ".xsl";
            rootOut.addContent(new Element("include", XSL).setAttribute("href", temppath));
        }

        String[] contentdir = checkForContent(rootOut);

        // first template named "chooseTemplate" in chooseTemplate.xsl
        Element template = new Element("template", XSL).setAttribute("name", "chooseTemplate");
        Element choose = new Element("choose", XSL);
        // second template named "get.templates" in chooseTemplate.xsl
        Element template2 = new Element("template", XSL).setAttribute("name", "get.templates");
        Element templates = new Element("templates");

        for (String aDirectory : directory) {
            temptest = "$template = '" + aDirectory + "'";

            // add elements in the first template
            Element when = new Element("when", XSL).setAttribute("test", temptest);
            Element call = new Element("call-template", XSL).setAttribute("name", aDirectory);
            when.addContent(call);
            choose.addContent(when);

            // add elements in the second template
            Element innertemplate = new Element("template").setAttribute("category", "master").setText(aDirectory);
            templates.addContent(innertemplate);
        }

        for (String aContentdir : contentdir) {
            Element innertemplate = new Element("template").setAttribute("category", "content").setText(aContentdir);
            templates.addContent(innertemplate);
        }

        // first
        template.addContent(choose);
        rootOut.addContent(template);
        // second
        template2.addContent(templates);
        rootOut.addContent(template2);

        // try to save the xsl stream
            XMLOutputter xmlOut = new XMLOutputter();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(choosepath));
                xmlOut.output(jdom, fos);
                fos.flush();
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
    }

    private String[] checkForContent(Element rootOut) {
        int newpath = templatepath.indexOf("master/");
        String newTemplatePath = templatepath.substring(0, newpath) + "content/";
        File templatedir = new File(newTemplatePath);
        if (!templatedir.exists() || templatedir.isFile())
            return new String[0];

        String[] contentdir = templatedir.list(new TemplateFilenameFilter());
        for (String aContentdir : contentdir) {
            String temppath = aContentdir + ".xsl";
            rootOut.addContent(new Element("include", XSL).setAttribute("href", temppath));
        }

        return contentdir;
    }

    // getter and setter for the templatepath and the choosepath

    public String getTemplatepath() {
        return templatepath;
    }

    public void setTemplatepath(String templatepath) {
        this.templatepath = templatepath;
    }

    public String getChoosepath() {
        return choosepath;
    }

    public void setChoosepath(String choosepath) {
        this.choosepath = choosepath;
    }
}
