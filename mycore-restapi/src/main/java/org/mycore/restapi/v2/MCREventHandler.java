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

package org.mycore.restapi.v2;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class MCREventHandler {

    private static String getName(MCREvent evt) {
        return evt.getObjectType() + "." + evt.getEventType();
    }

    private static URI getPathURI(String path) {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new MCRException(e);
        }
    }

    private static String getId(MCREvent evt) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
        byte[] bytes = byteBuffer.putLong(System.currentTimeMillis())
            .putInt(evt.hashCode())
            .array();
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(bytes);
    }

    private static void addUserInfo(JsonObject jEvent) {
        if (!MCRSessionMgr.hasCurrentSession()) {
            return;
        }
        String userID = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        jEvent.addProperty("user", userID);
    }

    public static class MCRObjectHandler implements org.mycore.common.events.MCREventHandler {
        private final SseBroadcaster sseBroadcaster;

        private final Sse sse;

        private final Function<URI, URI> uriResolver;

        public MCRObjectHandler(SseBroadcaster sseBroadcaster, Sse sse,
            Function<URI, URI> uriResolver) {
            this.sseBroadcaster = sseBroadcaster;
            this.sse = sse;
            this.uriResolver = uriResolver;
        }

        @Override
        public void doHandleEvent(MCREvent evt) throws MCRException {
            if (!evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) {
                return;
            }
            MCRObject obj = (MCRObject) evt.get(MCREvent.OBJECT_KEY);
            JsonObject jEvent = new JsonObject();
            JsonObject newData = getData(obj);
            addUserInfo(jEvent);
            jEvent.add("current", newData);
            MCRObject oldObj = (MCRObject) evt.get(MCREvent.OBJECT_OLD_KEY);
            if (oldObj != null) {
                JsonObject oldData = getData(oldObj);
                jEvent.add("old", oldData);
            }
            OutboundSseEvent event = sse.newEventBuilder()
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .id(getId(evt))
                .name(getName(evt))
                .data(jEvent.toString())
                .build();
            sseBroadcaster.broadcast(event);
        }

        private JsonObject getData(MCRObject obj) {
            JsonParser parser = new JsonParser();
            JsonObject event = new JsonObject();
            event.addProperty("id", obj.getId().toString());
            event.addProperty("uri", uriResolver.apply(getPathURI("objects/" + obj.getId())).toString());
            event.addProperty("state", obj.getService().getState().getID());
            event.addProperty("createdBy", obj.getService().getFlags("createdby").get(0));
            event.addProperty("created", obj.getService().getDate("createdate").toInstant().toString());
            event.addProperty("modifiedBy", obj.getService().getFlags("modifiedby").get(0));
            event.addProperty("modified", obj.getService().getDate("modifydate").toInstant().toString());
            JsonArray pi = new JsonArray();
            obj.getService().getFlags("MyCoRe-PI").stream()
                .map(parser::parse)
                .forEach(pi::add);
            event.add("pi", pi);
            return event;
        }

        @Override
        public void undoHandleEvent(MCREvent evt) throws MCRException {
            //do nothing
        }
    }

    public static class MCRDerivateHandler implements org.mycore.common.events.MCREventHandler {
        private final SseBroadcaster sseBroadcaster;

        private final Sse sse;

        private final Function<URI, URI> uriResolver;

        public MCRDerivateHandler(SseBroadcaster sseBroadcaster, Sse sse,
            Function<URI, URI> uriResolver) {
            this.sseBroadcaster = sseBroadcaster;
            this.sse = sse;
            this.uriResolver = uriResolver;
        }

        @Override
        public void doHandleEvent(MCREvent evt) throws MCRException {
            if (!evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
                return;
            }
            MCRDerivate der = (MCRDerivate) evt.get(MCREvent.DERIVATE_KEY);
            JsonObject jEvent = new JsonObject();
            addUserInfo(jEvent);
            JsonObject newData = getData(der);
            jEvent.add("current", newData);
            MCRDerivate oldDer = (MCRDerivate) evt.get(MCREvent.DERIVATE_OLD_KEY);
            if (oldDer != null) {
                JsonObject oldData = getData(oldDer);
                jEvent.add("old", oldData);
            }
            OutboundSseEvent event = sse.newEventBuilder()
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .id(getId(evt))
                .name(getName(evt))
                .data(jEvent.toString())
                .build();
            sseBroadcaster.broadcast(event);
        }

        private JsonObject getData(MCRDerivate der) {
            JsonObject event = new JsonObject();
            event.addProperty("id", der.getId().toString());
            event.addProperty("uri",
                uriResolver.apply(getPathURI("objects/" + der.getOwnerID())).toString() + "/derivates/" + der.getId());
            event.addProperty("object", der.getOwnerID().toString());
            event.addProperty("objectUri", uriResolver.apply(getPathURI("objects/" + der.getOwnerID())).toString());
            event.addProperty("createdBy", der.getService().getFlags("createdby").get(0));
            event.addProperty("created", der.getService().getDate("createdate").toInstant().toString());
            event.addProperty("modifiedBy", der.getService().getFlags("modifiedby").get(0));
            event.addProperty("modified", der.getService().getDate("modifydate").toInstant().toString());
            return event;
        }

        @Override
        public void undoHandleEvent(MCREvent evt) throws MCRException {
            //do nothing
        }
    }

    public static class MCRPathHandler extends MCREventHandlerBase {
        private final SseBroadcaster sseBroadcaster;

        private final Sse sse;

        private final ServletContext context;

        private final Function<URI, URI> uriResolver;

        public MCRPathHandler(SseBroadcaster sseBroadcaster, Sse sse,
            Function<URI, URI> uriResolver, ServletContext context) {
            this.sseBroadcaster = sseBroadcaster;
            this.sse = sse;
            this.context = context;
            this.uriResolver = uriResolver;
        }

        @Override
        public void doHandleEvent(MCREvent evt) throws MCRException {
            if (!evt.getObjectType().equals(MCREvent.PATH_TYPE)) {
                return;
            }
            super.doHandleEvent(evt);
        }

        @Override
        protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            sendEvent(evt, path, attrs);
        }

        @Override
        protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
            sendEvent(evt, path, attrs);
        }

        @Override
        protected void handlePathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
            sendEvent(evt, path, attrs);
        }

        @Override
        protected void updatePathIndex(MCREvent evt, Path path, BasicFileAttributes attrs) {
            sendEvent(evt, path, attrs);
        }

        @Override
        protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            sendEvent(evt, path, attrs);
        }

        public void sendEvent(MCREvent evt, Path path, BasicFileAttributes attrs) {
            if (!(path instanceof MCRPath) || !(attrs instanceof MCRFileAttributes)) {
                LogManager.getLogger().warn("Cannot handle {} {}", path.getClass(), attrs.getClass());
                return;
            }
            JsonObject file = new JsonObject();
            addUserInfo(file);
            String derId = ((MCRPath) path).getOwner();
            String fPath = ((MCRPath) path).getOwnerRelativePath();
            String objId = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(derId), 1, TimeUnit.MINUTES)
                .toString();
            String relPath = String.format(Locale.ROOT, "objects/%s/derivates/%s/contents/%s", objId, derId, fPath);
            String uri = uriResolver.apply(getPathURI(relPath)).toString();
            file.addProperty("uri", uri);
            file.addProperty("derivate", derId);
            file.addProperty("object", objId);
            file.addProperty("size", attrs.size());
            file.addProperty("md5", ((MCRFileAttributes) attrs).md5sum());
            file.addProperty("mimeType", context.getMimeType(path.getFileName().toString()));
            OutboundSseEvent event = sse.newEventBuilder()
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .id(getId(evt))
                .name(getName(evt))
                .data(file.toString())
                .build();
            sseBroadcaster.broadcast(event);
        }

        @Override
        public void undoHandleEvent(MCREvent evt) throws MCRException {
            //do nothing
        }
    }

}
