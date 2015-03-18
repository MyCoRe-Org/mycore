package org.mycore.migration2014.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

@MCRCommandGroup(name = "MCRDeprecated Commands")
public class MCRDeprecatedCommands {

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRDeprecatedCommands.class.getName());

    /**
     * Delete all selected MCRObjects from the datastore.
     */
    @MCRCommand(syntax = "delete selected", help = "Removes selected MCRObjects.", order = 200)
    public static void deleteSelected() throws MCRActiveLinkException {
        LOGGER.info("Start removing selected MCRObjects");
        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();
        if (selectedObjectIDs.isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }
        for (String id : selectedObjectIDs) {
            MCRObjectCommands.delete(id);
        }
        LOGGER.info("Selected MCRObjects deleted");
    }

    /**
     * Does a xsl transform with selected MCRObjects.
     * 
     * @param xslFilePath file to transform the objects
     * @return a list of transform commands
     * @throws MCRActiveLinkException
     * @see {@link #xslt(String, String)}
     */
    @MCRCommand(syntax = "transform selected with file {0}", help = "xsl transforms selected MCRObjects", order = 290)
    public static List<String> transformSelected(String xslFilePath) {
        LOGGER.info("Start transforming selected MCRObjects");
        File xslFile = new File(xslFilePath);
        if (!xslFile.exists()) {
            LOGGER.error("XSLT file not found " + xslFilePath);
            return new ArrayList<String>();
        }
        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();
        if (selectedObjectIDs.isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return new ArrayList<String>();
        }
        List<String> commandList = new ArrayList<String>();
        for (String mcrId : selectedObjectIDs) {
            commandList.add("xslt " + mcrId + " with file " + xslFilePath);
        }
        return commandList;
    }

    /**
     * Export selected MCRObjects to a file named <em>MCRObjectID</em> .xml in a directory. The method use the converter stylesheet
     * mcr_<em>style</em>_object.xsl.
     * @param dirname the dirname to store the object @param style the type of the stylesheet
     */
    @MCRCommand(syntax = "export selected to directory {0} with {1}", help = "Stores selected MCRObjects to the directory {0} with the stylesheet {1}-object.xsl. For {1} save is the default.", order = 210)
    public static List<String> exportSelected(String dirname, String style) {
        LOGGER.info("Start exporting selected MCRObjects");

        List<String> selectedObjectIDs = MCRObjectCommands.getSelectedObjectIDs();
        if (null == selectedObjectIDs) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<String>(selectedObjectIDs.size());
        for (String id : selectedObjectIDs) {
            cmds.add("export object from " + id + " to " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

}
