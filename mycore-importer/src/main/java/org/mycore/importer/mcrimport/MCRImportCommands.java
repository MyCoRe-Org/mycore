package org.mycore.importer.mcrimport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImportCommands extends MCRAbstractCommands {

    public MCRImportCommands() {
        super();

        MCRCommand importFromMappingFile = new MCRCommand("import from mapping file {0}", "org.mycore.importer.mcrimport.MCRImportCommands.startImport String", "");
        addCommand(importFromMappingFile);

        MCRCommand importDerivate = new MCRCommand("internal import derivate {0} and upload files {1}", "org.mycore.importer.mcrimport.MCRImportCommands.importDerivate String String", "");
        addCommand(importDerivate);

        MCRCommand uploadFile = new MCRCommand("internal upload file {0} for derivate {1}", "org.mycore.importer.mcrimport.MCRImportCommands.uploadFile String String", "");
        addCommand(uploadFile);
    }

    public static List<String> startImport(String file) throws Exception {
        File mappingFile = new File(file);
        MCRImportImporter importer = new MCRImportImporter(mappingFile);
        importer.generateMyCoReFiles();
        return importer.getCommandList();
    }

    /**
     * This method imports a derivate to mycore.
     * 
     * @param pathToDerivate path to the import derivate
     * @param uploadFiles if true, all internal files will be uploaded
     */
    @SuppressWarnings("unchecked")
    public static List<String> importDerivate(String pathToDerivate, String uploadFiles) throws Exception {
        // load the xml document
        SAXBuilder builder = new SAXBuilder();
        Element rootElement = builder.build(new File(pathToDerivate)).getRootElement();
        
        // create a new derivate and set id and label
        MCRDerivate derivate = new MCRDerivate();
        String id = rootElement.getAttributeValue("ID");
        String label = rootElement.getAttributeValue("label");
        derivate.setId(MCRObjectID.getInstance(id));
        derivate.setLabel(label);

        // set the schema
        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        // set the linked object
        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        Element linkmetasElement = rootElement.getChild("linkmetas");
        Element linkmetaElement = linkmetasElement.getChild("linkmeta");
        String href = linkmetaElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE); 
        linkId.setReference(href, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        // set default meta ifs internal
        Element filesElement = rootElement.getChild("files");
        String mainDoc = null;
        if(filesElement != null)
            mainDoc = filesElement.getAttributeValue("mainDoc");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        ifs.setMainDoc(mainDoc);
        derivate.getDerivate().setInternals(ifs);

        // create in db
        MCRMetadataManager.create(derivate);

        // if upload files active? if true return a list of upload commands
        List<String> commandList = new ArrayList<String>();
        
        if(Boolean.valueOf(uploadFiles) && filesElement != null) {
            for(Element fileElement : (List<Element>)filesElement.getChildren("file")) {
                String path = fileElement.getText();
                StringBuilder command = new StringBuilder("internal upload file ");
                command.append(path);
                command.append(" for derivate ");
                command.append(id);
                commandList.add(command.toString());
            }
        }
        return commandList;
    }

    /**
     * This method does the file (jpg, tiff, pdf...) upload for the importer.
     * It uses the <code>addFiles</code> method from <code>MCRFileImportExport</code>
     * utility class.
     * 
     * @see MCRFileImportExport#addFiles(File, String)
     * @param path could be a directory or a file which is uploaded
     * @param derivateId where the files are attached
     * @throws Exception
     */
    public static void uploadFile(String path, String derivateId) throws Exception {
        MCRFileImportExport.addFiles(new File(path), derivateId);
    }

}