package org.mycore.importer.mcrimport;

import java.io.File;
import java.util.List;

import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

public class MCRImportCommands extends MCRAbstractCommands {

    public MCRImportCommands() {
        super();

        MCRCommand importFromMappingFile = new MCRCommand("import from mapping file {0}", "org.mycore.importer.mcrimport.MCRImportCommands.startImport String", "");
        command.add(importFromMappingFile);
        
        MCRCommand importInternalDerivateFile = new MCRCommand("import file of derivate {0} {1}", "org.mycore.importer.mcrimport.MCRImportCommands.importInternalDerivateFile String String", "");
        command.add(importInternalDerivateFile);
    }

    public static List<String> startImport(String file) throws Exception {
        File mappingFile = new File(file);
        MCRImportImporter importer = new MCRImportImporter(mappingFile);
        importer.generateMyCoReFiles();
        return importer.getCommandList();
    }

    /**
     * This method does the file (jpg, tiff, pdf...) upload for the importer.
     * It uses the <code>addFiles</code> method from <code>MCRFileImportExport</code>
     * utility class.
     * 
     * @see MCRFileImportExport#addFiles(File, String)
     * @param derivateId file status of the current mycore derivate
     */
    protected void importInternalDerivateFile(String derivateId, String path) {
        MCRFileImportExport.addFiles(new File(path), derivateId);
    }

}