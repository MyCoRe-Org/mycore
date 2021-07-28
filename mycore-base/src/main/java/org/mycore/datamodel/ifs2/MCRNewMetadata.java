package org.mycore.datamodel.ifs2;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * A generic class to interact with metadata stored in a
 * {@link MCRNewMetadataStore}-based store.
 * 
 * A MCRNewMetadata instance represents an object that is (to be) stored in the
 * linked store. It has an immutable set of information about its associated
 * store, its name and its ID.
 * 
 * @author Christoph Neidahl (OPNA2608)
 *
 */
public class MCRNewMetadata {

    protected static final Logger LOGGER = LogManager.getLogger(MCRNewMetadata.class);

    private final MCRNewMetadataStore store;

    private final String docType;

    private final MCRObjectID id;

    private String revision;

    private MCRContent content;

    private Date date;

    public MCRNewMetadata(MCRNewMetadataStore store, int id) {
        this.store = store;
        docType = store.getDocType();
        this.id = MCRObjectID.getInstance(MCRObjectID.formatID(store.getID(), id));
    }

    public MCRNewMetadata(MCRNewMetadataStore store, int id, String revision) {
        this(store, id);
        this.revision = revision;
    }

    public void create(MCRContent content) throws MCRPersistenceException {
        try {
            store.createContent(this, content);
            this.revision = store.getVersionLast(this).getRevision();
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException(
                "Failed to create " + id.toString() + " in revision " + revision + " in store!", e);
        }
    }

    public MCRContent read() throws MCRPersistenceException {
        try {
            if (this.revision == null) {
                this.revision = store.getVersionLast(this).getRevision();
            }
            this.content = store.readContent(this);
            return this.content;
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException(
                "Failed to read " + id.toString() + " in revision " + revision + " from store!", e);
        }
    }

    public void update(MCRContent content) throws MCRPersistenceException {
        try {
            store.updateContent(this, content);
            this.revision = store.getVersionLast(this).getRevision();
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException(
                "Failed to update " + id.toString() + " in revision " + revision + " in store!", e);
        }
    }

    public void delete() throws MCRPersistenceException {
        try {
            store.deleteContent(this);
            this.revision = store.getVersionLast(this).getRevision();
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException(
                "Failed to delete " + id.toString() + " in revision " + revision + " from store!", e);
        }
    }

    public String getBase() {
        return id.getBase();
    }

    public int getID() {
        return id.getNumberAsInteger();
    }

    public MCRObjectID getFullID() {
        return id;
    }

    public String getDocType() {
        return docType;
    }

    public String getRevision() {
        return revision;
    }

    public Date getDate() {
        return date;
    }

    void setRevision(String revision) {
        this.revision = revision;
    }

    public MCRContent getContent() {
        return this.content;
    }
}
