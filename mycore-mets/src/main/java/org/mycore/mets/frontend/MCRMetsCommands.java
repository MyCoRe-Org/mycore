package org.mycore.mets.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mets.model.MCRMETSGenerator;
import org.mycore.mets.tools.MCRMetsSave;
import org.mycore.mets.validator.METSValidator;
import org.mycore.mets.validator.validators.ValidationException;

@MCRCommandGroup(name = "MCR Mets Commands")
public class MCRMetsCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRMetsCommands.class);

    @MCRCommand(syntax = "validate selected mets", help = "validates all mets.xml of selected derivates", order = 10)
    public static void validateSelectedMets() {
        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();

        for (String objectID : selectedObjectIDs) {
            LOGGER.info("Validate mets.xml of " + objectID);
            MCRPath metsFile = MCRPath.getPath(objectID, "/mets.xml");
            if (Files.exists(metsFile)) {
                try {
                    MCRContent content = new MCRPathContent(metsFile);
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

	@MCRCommand(syntax = "add mets files for derivate {0}", help = "", order = 20)
	public static void addMetsFileForDerivate(String derivateID) {
		MCRPath metsFile = MCRPath.getPath(derivateID, "/mets.xml");
		if (!Files.exists(metsFile)) {
			try {
	            LOGGER.debug("Start MCRMETSGenerator for derivate " + derivateID);
				HashSet<MCRPath> ignoreNodes = new HashSet<MCRPath>();
				Document mets = MCRMETSGenerator.getGenerator()
						.getMETS(MCRPath.getPath(derivateID, "/"), ignoreNodes)
						.asDocument();
				MCRMetsSave.saveMets(mets, MCRObjectID.getInstance(derivateID));
	            LOGGER.debug("Stop MCRMETSGenerator for derivate " + derivateID);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				LOGGER.error("Can't create mets file for derivate "
						+ derivateID);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				LOGGER.error("Can't create mets file for derivate "
						+ derivateID);
			} catch (InstantiationException e) {
				e.printStackTrace();
				LOGGER.error("Can't create mets file for derivate "
						+ derivateID);
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Can't create mets file for derivate "
						+ derivateID);
			}
		}
	}
	
	@MCRCommand(syntax = "add mets files for project id {0}", help = "", order = 30)
	public static void addMetsFileForProjectID(String projectID) {
		MCRXMLMetadataManager manager = MCRXMLMetadataManager.instance();
		List <String> dervate_list = manager.listIDsForBase(projectID + "_derivate");
		for (String derivateID : dervate_list) {
			addMetsFileForDerivate(derivateID);
		}
	}
}
