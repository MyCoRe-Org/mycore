/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mets.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mets.model.MCRMETSGeneratorFactory;
import org.mycore.mets.model.converter.MCRSimpleModelXMLConverter;
import org.mycore.mets.model.converter.MCRXMLSimpleModelConverter;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;
import org.mycore.mets.tools.MCRMetsSave;
import org.mycore.mets.validator.METSValidator;
import org.mycore.mets.validator.validators.ValidationException;

@MCRCommandGroup(name = "Mets Commands")
public class MCRMetsCommands extends MCRAbstractCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCRMetsCommands.class);

    public static ConcurrentLinkedQueue<String> invalidMetsQueue = new ConcurrentLinkedQueue<>();

    @MCRCommand(syntax = "validate selected mets", help = "validates all mets.xml of selected derivates", order = 10)
    public static void validateSelectedMets() {
        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();

        for (String objectID : selectedObjectIDs) {
            LOGGER.info("Validate mets.xml of {}", objectID);
            MCRPath metsFile = MCRPath.getPath(objectID, "/mets.xml");
            if (Files.exists(metsFile)) {
                try {
                    MCRContent content = new MCRPathContent(metsFile);
                    InputStream metsIS = content.getInputStream();
                    METSValidator mv = new METSValidator(metsIS);
                    List<ValidationException> validationExceptionList = mv.validate();
                    if (validationExceptionList.size() > 0) {
                        invalidMetsQueue.add(objectID);
                    }
                    for (ValidationException validationException : validationExceptionList) {
                        LOGGER.error(validationException.getMessage());
                    }
                } catch (IOException e) {
                    LOGGER.error("Error while reading mets.xml of {}", objectID, e);
                } catch (JDOMException e) {
                    LOGGER.error("Error while parsing mets.xml of {}", objectID, e);
                }
            }

        }
    }

    @MCRCommand(syntax = "try fix invalid mets",
        help = "This Command can be used to fix invalid mets files that was found in any validate selected mets runs.",
        order = 15)
    public static void fixInvalidMets() {
        String selectedObjectID;
        while ((selectedObjectID = invalidMetsQueue.poll()) != null) {
            LOGGER.info("Try to fix METS of {}", selectedObjectID);
            MCRPath metsFile = MCRPath.getPath(selectedObjectID, "/mets.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document metsDocument;
            try (InputStream metsInputStream = Files.newInputStream(metsFile)) {
                metsDocument = saxBuilder.build(metsInputStream);
            } catch (IOException | JDOMException e) {
                LOGGER.error(MessageFormat.format("Cannot fix METS of {0}. Can not parse mets.xml!", selectedObjectID),
                    e);
                return;
            }

            MCRMetsSimpleModel mcrMetsSimpleModel;
            try {
                mcrMetsSimpleModel = MCRXMLSimpleModelConverter.fromXML(metsDocument);
            } catch (Exception e) {
                LOGGER.error(
                    MessageFormat.format("Cannot fix METS of {0}. Can not convert to SimpleModel!", selectedObjectID),
                    e);
                return;
            }

            Document newMets = MCRSimpleModelXMLConverter.toXML(mcrMetsSimpleModel);
            XMLOutputter xmlOutputter = new XMLOutputter();
            try (OutputStream os = Files.newOutputStream(metsFile)) {
                xmlOutputter.output(newMets, os);
            } catch (IOException e) {
                LOGGER.error(
                    MessageFormat.format("Cannot fix METS of {0}. Can not write mets to derivate.", selectedObjectID));
            }
        }
    }

    @MCRCommand(syntax = "add mets files for derivate {0}", order = 20)
    public static void addMetsFileForDerivate(String derivateID) {
        MCRPath metsFile = MCRPath.getPath(derivateID, "/mets.xml");
        if (!Files.exists(metsFile)) {
            try {
                LOGGER.debug("Start MCRMETSGenerator for derivate {}", derivateID);
                Document mets = MCRMETSGeneratorFactory.create(MCRPath.getPath(derivateID, "/")).generate()
                    .asDocument();
                MCRMetsSave.saveMets(mets, MCRObjectID.getInstance(derivateID));
                LOGGER.debug("Stop MCRMETSGenerator for derivate {}", derivateID);
            } catch (Exception e) {
                LOGGER.error("Can't create mets file for derivate {}", derivateID);
            }
        }
    }

    @MCRCommand(syntax = "add mets files for project id {0}", order = 30)
    public static void addMetsFileForProjectID(String projectID) {
        MCRXMLMetadataManager manager = MCRXMLMetadataManager.instance();
        List<String> dervate_list = manager.listIDsForBase(projectID + "_derivate");
        for (String derivateID : dervate_list) {
            addMetsFileForDerivate(derivateID);
        }
    }
}
