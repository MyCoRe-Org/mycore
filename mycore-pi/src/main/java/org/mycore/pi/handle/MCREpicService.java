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

package org.mycore.pi.handle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.jdom2.Document;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.MCRPIJobService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCREpicService extends MCRPIJobService<MCRHandle> {

    public static final String EPIC_KEY = "EPIC";

    public static final String OBJECT_ID_KEY = "ObjectID";

    /**
     * The Username which will be used by the epic client
     */
    @MCRProperty(name = "Username")
    public String username;

    /**
     * The password which will be used by the epic client
     */
    @MCRProperty(name = "Password")
    public String password;

    /**
     * The url to the actual epic api endpoint e.g. https://epic.grnet.gr/api/v2/, http://pid.gwdg.de/
     */
    @MCRProperty(name = "Endpoint")
    public String endpoint;

    /**
     * This is a alternative to mcr.baseurl mostly for testing purposes
     */
    @MCRProperty(name = "BaseURL", required = false)
    public String baseURL;

    /**
     * This can be used to store metadata as a Handle Object. The Transformer will be used to convert the Object to an
     * String.
     */
    @MCRProperty(name = "Transformer", required = false)
    public String transformerID = null;

    /**
     * The Type which should be used in the Handle Object.
     */
    @MCRProperty(name = "MetadataType", required = false)
    public String metadataType = null;

    /**
     * The Index which should be used in the handle object.
     */
    @MCRProperty(name = "MetadataIndex", required = false)
    public String metadataIndex = null;

    public ConcurrentHashMap<String, ReentrantLock> idLockMap = new ConcurrentHashMap<>();

    public MCREpicService() {
        super("handle");
    }

    @Override
    protected boolean validateRegistrationDocument(MCRBase obj, MCRHandle identifier, String additional) {
        return true;
    }

    @Override
    protected void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String epic = parameters.get(EPIC_KEY);

        try {
            getClient().delete(new MCRHandle(epic));
        } catch (IOException e) {
            throw new MCRPersistentIdentifierException("Error while communicating with epic service", e);
        }
    }

    @Override
    protected void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String epic = parameters.get(EPIC_KEY);
        String objId = parameters.get(OBJECT_ID_KEY);

        createOrUpdate(epic, objId);
    }

    @Override
    protected void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String epic = parameters.get(EPIC_KEY);
        String objId = parameters.get(OBJECT_ID_KEY);

        createOrUpdate(epic, objId);
    }

    private void createOrUpdate(String epic, String objId) throws MCRPersistentIdentifierException {
        new ReentrantLock();

        final MCRObjectID objectID = MCRObjectID.getInstance(objId);
        if (!MCRMetadataManager.exists(objectID)) {
            return;
        }

        validateJobUserRights(objectID);

        final MCRHandle mcrHandle = new MCRHandle(epic);
        final String urlForObject = getURLForObject(objId);

        try {
            final ArrayList<MCRHandleInfo> handleInfos = new ArrayList<>();
            processMedataData(objectID, handleInfos);

            ReentrantLock reentrantLock = idLockMap.computeIfAbsent(epic, (l) -> new ReentrantLock());
            try {
                reentrantLock.lock();
                getClient().create(urlForObject, mcrHandle, handleInfos);
            } finally {
                reentrantLock.unlock();
            }
        } catch (IOException e) {
            throw new MCRPersistentIdentifierException("Error while communicating with EPIC Service", e);
        }

    }

    private void processMedataData(MCRObjectID objectID, ArrayList<MCRHandleInfo> handleInfos) throws IOException {
        if (transformerID != null && metadataType != null && metadataIndex != null) {
            final int index = Integer.parseInt(metadataIndex, 10);
            final Document xml = MCRMetadataManager.retrieve(objectID).createXML();
            final MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer(transformerID);
            final MCRContent result = transformer.transform(new MCRJDOMContent(xml));
            final byte[] bytes = result.asByteArray();
            final String encodedData = Base64.getEncoder().encodeToString(bytes);

            final MCRHandleInfo metadataInfo = new MCRHandleInfo();
            metadataInfo.setIdx(index);
            metadataInfo.setData(encodedData);
            metadataInfo.setType(metadataType);
            handleInfos.add(metadataInfo);
        }
    }

    protected String getURLForObject(String objectId) {
        String baseURL = this.baseURL != null ? this.baseURL : MCRFrontendUtil.getBaseURL();
        return baseURL + "receive/" + objectId;

    }

    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        return Optional.empty();
    }

    @Override
    protected HashMap<String, String> createJobContextParams(PiJobAction action, MCRBase obj, MCRHandle epic) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(EPIC_KEY, epic.asString());
        contextParameters.put(OBJECT_ID_KEY, obj.getId().toString());
        return contextParameters;
    }

    private MCREpicClient getClient() {
        return new MCREpicClient(username, password, endpoint);
    }

}
