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

package org.mycore.mods.enrichment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mods.MCRMODSWrapper;

/**
 * When a new MCRObject is created which contains MODS publication data,
 * and that publications is marked as CC licensed, and there is a PDF url given,
 * imports the PDF from the URL and stores it in a newly created derivate.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRImportCCPublications extends MCREventHandlerBase {

    private static final String PATTERN_CC_LICENSE = "#cc";

    private static final String PATTERN_LICENSE = "license";

    private static final String HREF = "href";

    private static final String MODS_ACCESS_CONDITION = "accessCondition";

    private static final String SUFFIX_PDF = ".pdf";

    private static final String MODS_URL = "url";

    private static final String MODS_LOCATION = "location";

    private static final String DATAMODEL_DEFAULT = "datamodel-derivate.xml";

    private static final String DATAMODEL_CONFIG = "MCR.Metadata.Config.derivate";

    private static final String SUBTAG_LINKMETA = "linkmeta";

    private static final String SUBTAG_INTERNAL = "internal";

    private static final String LABEL_PREFIX = "imported from ";

    private static final String TYPE_DERIVATE = "derivate";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        if (!MCRMODSWrapper.isSupported(obj)) {
            return;
        }

        Element mods = new MCRMODSWrapper(obj).getMODS();
        if (!isCCLicensed(mods)) {
            return;
        }

        URL url = getURLofPDF(mods);
        if (url == null) {
            return;
        }

        MCRObjectID objectID = obj.getId();
        MCRObjectID derivateID = MCRObjectID.getNextFreeId(objectID.getProjectId() + '_' + TYPE_DERIVATE);

        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.setLabel(LABEL_PREFIX + url.toExternalForm());

        setSchema(derivate);
        linkToObject(objectID, derivate);

        String fileName = getFileName(url);

        MCRMetaIFS ifs = buildMetaIFS(fileName);

        try {
            MCRPath rootDir = createRootDir(derivateID);
            setIFSID(ifs, rootDir);
            derivate.getDerivate().setInternals(ifs);

            MCRMetadataManager.create(derivate);

            importPDF(rootDir, fileName, url);
        } catch (MCRPersistenceException | MCRAccessException | IOException ex) {
            LOGGER.warn(ex);
        }
    }

    private MCRMetaIFS buildMetaIFS(String fileName) {
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag(SUBTAG_INTERNAL);
        ifs.setSourcePath(null);
        ifs.setMainDoc(fileName);
        return ifs;
    }

    private void linkToObject(MCRObjectID objectID, MCRDerivate derivate) {
        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag(SUBTAG_LINKMETA);
        linkId.setReference(objectID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);
    }

    private void setSchema(MCRDerivate derivate) {
        String schema = MCRConfiguration.instance().getString(DATAMODEL_CONFIG, DATAMODEL_DEFAULT);
        derivate.setSchema(schema.replaceAll(".xml", ".xsd"));
    }

    private void setIFSID(MCRMetaIFS ifs, MCRPath rootDir) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(rootDir, BasicFileAttributes.class);
        ifs.setIFSID(attrs.fileKey().toString());
    }

    private MCRPath createRootDir(MCRObjectID derivateID) throws FileSystemException {
        MCRPath rootDir = MCRPath.getPath(derivateID.toString(), "/");
        rootDir.getFileSystem().createRoot(derivateID.toString());
        return rootDir;
    }

    private void importPDF(MCRPath rootDir, String fileName, URL url)
        throws MalformedURLException, IOException {
        MCRPath file = MCRPath.toMCRPath(rootDir.resolve(fileName));

        MCRContent content = new MCRURLContent(url);
        MCRContentInputStream in = content.getContentInputStream();

        Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        in.close();
    }

    private URL getURLofPDF(Element mods) {
        for (Element mLocation : mods.getChildren(MODS_LOCATION, MCRConstants.MODS_NAMESPACE)) {
            for (Element mURL : mLocation.getChildren(MODS_URL, MCRConstants.MODS_NAMESPACE)) {
                String text = mURL.getTextTrim();
                if (text.toLowerCase().endsWith(SUFFIX_PDF)) {
                    try {
                        return new URL(text);
                    } catch (MalformedURLException ignored) {
                        LOGGER.warn("Malformed PDF URL: {}", text);
                    }
                }
            }
        }
        return null;
    }

    private boolean isCCLicensed(Element mods) {
        for (Element accessCondition : mods.getChildren(MODS_ACCESS_CONDITION, MCRConstants.MODS_NAMESPACE)) {
            String href = accessCondition.getAttributeValue(HREF, MCRConstants.XLINK_NAMESPACE);
            if ((href != null)
                && href.toLowerCase().contains(PATTERN_LICENSE)
                && href.toLowerCase().contains(PATTERN_CC_LICENSE)) {
                return true;
            }
        }
        return false;
    }

    private String getFileName(URL url) {
        String[] fragments = url.getPath().split("/");
        return fragments[fragments.length - 1];
    }
}
