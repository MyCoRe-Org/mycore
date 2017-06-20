package org.mycore.solr.index.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRCache;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.io.Files;

public class MCRSolrFileIndexBaseAccumulator implements MCRSolrFileIndexAccumulator {

    private static Logger LOGGER = LogManager.getLogger(MCRSolrFileIndexBaseAccumulator.class);

    private static MCRXMLMetadataManager XML_MANAGER = MCRXMLMetadataManager.instance();

    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCache<String, String> derivateModified = new MCRCache<>(10000,
        "derivateID ISODateString cache");

    @Override
    public void accumulate(SolrInputDocument doc, Path input, BasicFileAttributes attr) throws IOException {
        doc.setField("id", input.toUri().toString());
        String absolutePath = '/' + input.subpath(0, input.getNameCount()).toString();
        try {
            MCRPath mcrPath = MCRPath.toMCRPath(input); //check if this is an MCRPath -> more metadata
            MCRObjectID mcrObjID = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(mcrPath.getOwner()), 10,
                TimeUnit.SECONDS);
            if (mcrObjID == null) {
                LOGGER.warn("Could not determine MCRObject for file " + absolutePath);
                doc.setField("returnId", mcrPath.getOwner());
            } else {
                doc.setField("returnId", mcrObjID.toString());
                doc.setField("objectProject", mcrObjID.getProjectId());
            }
            String ownerID = mcrPath.getOwner();
            doc.setField("derivateID", ownerID);
            doc.setField("derivateModified", getDerivateModified(ownerID));
            Collection<MCRCategoryID> linksFromReference = MCRCategLinkServiceFactory.getInstance()
                .getLinksFromReference(new MCRCategLinkReference(mcrPath));
            HashSet<MCRCategoryID> linkedCategories = new HashSet<>(linksFromReference);
            for (MCRCategoryID category : linksFromReference) {
                for (MCRCategory parent : CATEGORY_DAO.getParents(category)) {
                    linkedCategories.add(parent.getId());
                }
            }
            for (MCRCategoryID category : linkedCategories) {
                doc.addField("fileCategory", category.toString());
            }
        } catch (ProviderMismatchException e) {
            LOGGER.warn("Cannot build all fields as input is not an instance of MCRPath: " + input);
        }
        doc.setField("objectType", "data_file");
        doc.setField("fileName", input.getFileName().toString());
        doc.setField("filePath", absolutePath);
        doc.setField("stream_size", attr.size());
        doc.setField("stream_name", absolutePath);
        doc.setField("stream_source_info", input.toString());
        doc.setField("stream_content_type", MCRContentTypes.probeContentType(input));
        doc.setField("extension", Files.getFileExtension(input.getFileName().toString()));
        MCRISO8601Date iDate = new MCRISO8601Date();
        iDate.setDate(new Date(attr.lastModifiedTime().toMillis()));
        doc.setField("modified", iDate.getISOString());
    }

    /**
     * returns ISO8601 formated string of when derivate was last modified
     *
     * @param derivateID
     * @throws IOException
     *             thrown by {@link MCRCache.ModifiedHandle#getLastModified()}
     */
    private static String getDerivateModified(final String derivateID) throws IOException {
        MCRObjectID derID = MCRObjectID.getInstance(derivateID);
        MCRCache.ModifiedHandle modifiedHandle = XML_MANAGER.getLastModifiedHandle(derID, 30, TimeUnit.SECONDS);
        String modified = derivateModified.getIfUpToDate(derivateID, modifiedHandle);
        if (modified == null) {
            Date date = new Date(modifiedHandle.getLastModified());
            MCRISO8601Date date2 = new MCRISO8601Date();
            date2.setDate(date);
            modified = date2.getISOString();
            derivateModified.put(derivateID, modified);
        }
        return modified;
    }
}
