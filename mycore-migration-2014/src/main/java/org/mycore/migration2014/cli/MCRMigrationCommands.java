package org.mycore.migration2014.cli;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jdom2.Comment;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mods.MCRMODSWrapper;

@MCRCommandGroup(name = "MCR Migration Commands from MyCoRe version 2.1 to 2.2")
public class MCRMigrationCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands.class);

    private static String MODS_COMMENT = "Inheritance fixed by \"migrate mods inheritance\".";

    public MCRMigrationCommands() {
    }

    @MCRCommand(help = "Migrates MODS inheritance for all MCRObjects of type \"mods\"", syntax = "migrate mods inheritance", order = 10)
    public static List<String> migrateMods() {
        LOGGER.info("Getting all mods documents that have children.");
        List<String> commands = new ArrayList<>();
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRLINKHREF.class);
        criteria.add(Restrictions.eq("key.mcrtype", MCRLinkTableManager.ENTRY_TYPE_PARENT));
        criteria.add(Restrictions.like("key.mcrto", "%_mods_%"));
        criteria.setProjection(Projections.distinct(Projections.property("key.mcrto")));
        @SuppressWarnings("unchecked")
        List<String> parentIdList = criteria.list();
        if (parentIdList.isEmpty()) {
            LOGGER
                .warn("Did not find any objects. You may need to execute \"repair metadata search of type mods\" first.");
        }
        for (String parentId : parentIdList) {
            String cmd = "migrate mods inheritance for " + parentId;
            commands.add(cmd);
        }
        return commands;
    }

    @MCRCommand(help = "Migrates MODS inheritance for MCRObject {0}", syntax = "migrate mods inheritance for {0}")
    public static void migrateMods(String objectId) throws MCRPersistenceException, MCRActiveLinkException {
        MCRObjectID parentId = MCRObjectID.getInstance(objectId);
        MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentId);
        if (parent.getStructure().getChildren().isEmpty()) {
            LOGGER.warn("Could not fix inheritance as " + parentId + " has no children.");
            return;
        }
        LOGGER.info("Updating " + parentId + " and children.");
        //need to change MODS-XML so that inheritance is triggered
        MCRMODSWrapper wrapper = new MCRMODSWrapper(parent);
        Element mods = wrapper.getMODS();
        List<Comment> content = mods.getContent(Filters.comment());
        for (Comment comment : content) {
            if (comment.getText().equals(MODS_COMMENT)) {
                LOGGER.warn("MCRObject " + parentId + " has already been migrated");
                return;
            }
        }
        mods.addContent(0, new Comment(MODS_COMMENT));
        MCRMetadataManager.update(parent);
    }
}
