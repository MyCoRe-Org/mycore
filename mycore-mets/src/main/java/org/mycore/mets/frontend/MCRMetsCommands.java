package org.mycore.mets.frontend;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mets.validator.METSValidator;
import org.mycore.mets.validator.validators.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@MCRCommandGroup(name = "MCR Mets Commands")
public class MCRMetsCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRMetsCommands.class);

    @MCRCommand(syntax = "validate selected mets", help = "validates all mets.xml of selected derivates", order = 10)
    public static void validateSelectedMets(){
        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();

        for (String objectID : selectedObjectIDs) {
            LOGGER.info("Validate mets.xml of " + objectID);
            MCRDirectory directory = MCRDirectory.getRootDirectory(objectID);
            MCRFilesystemNode metsFile = directory.getChildByPath("mets.xml");
            if (metsFile != null) {
                try {
                    MCRContent content = ((MCRFile) metsFile).getContent();
                    InputStream metsIS = content.getInputStream();
                    METSValidator mv = new METSValidator(metsIS);
                    List<ValidationException> validationExceptionList = mv.validate();
                    for (ValidationException validationException : validationExceptionList) {
                        LOGGER.error(validationException.getMessage());
                    }
                } catch (IOException e) {
                   LOGGER.error("Error while reading mets.xml of " + objectID, e);
                } catch (JDOMException e) {
                   LOGGER.error("Error while parsing mets.xml of " + objectID, e);
                }
            }

        }
    }

}
