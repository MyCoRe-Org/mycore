package org.mycore.importer.mcrimport.classification;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;


/**
 * This class browses through the given path and replaces
 * the old classification values from the source with the
 * new from the classification mapping files. 
 * 
 * @author Matthias Eichner
 */
public class MCRImportClassificationReplacer {

    private static final Logger LOGGER = Logger.getLogger(MCRImportClassificationReplacer.class);
    
    public void replaceAll(File folder) {
        // go through all files and directories
        for(File child : folder.listFiles()) {
            // if the child is directory, call this method recursive
            if(child.isDirectory())
                replaceAll(child);
            else
                // try to replace the classification values
                replaceClassificationValuesInFile(child);
        }
    }

    protected void replaceClassificationValuesInFile(File file) {
        if(!file.getAbsolutePath().endsWith(".xml")) {
            LOGGER.warn(file + " is not a xml file");
            return;
        }

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(file);
        } catch(Exception exc) {
            LOGGER.error(exc);
            return;
        }

        Iterator<Element> it = doc.getDescendants(new ClassificationFilter());
        while(it.hasNext()) {
            Element classElement = it.next();
            String classId = classElement.getAttributeValue("classid");
            String categId = classElement.getAttributeValue("categid");
            String newValue = MCRImportClassificationMappingManager.getInstance().getMyCoReValue(classId, categId);
            if(newValue != null && !newValue.equals("") && !newValue.equals(categId))
                classElement.setAttribute("categid", newValue);
        }
    }

    private class ClassificationFilter implements Filter {
        @Override
        public boolean matches(Object arg0) {
            if(!(arg0 instanceof Element))
                return false;
            Element e = (Element)arg0;
            Element p = e.getParentElement();
            if(p == null)
                return false;
            if(!"MCRMetaClassification".equals(p.getAttributeValue("class")))
                return false;
            return true;
        }
    }
}