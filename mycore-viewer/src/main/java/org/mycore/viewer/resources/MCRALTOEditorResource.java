package org.mycore.viewer.resources;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.viewer.alto.MCRALTOUtil;
import org.mycore.viewer.alto.model.MCRAltoChangePID;
import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;
import org.mycore.viewer.alto.service.MCRAltoChangeApplier;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;

@Path("/viewer/alto")

public class MCRALTOEditorResource {

    @Inject
    private MCRAltoChangeSetStore changeSetStore;

    @Inject
    private MCRAltoChangeApplier changeApplier;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/store")
    public MCRAltoChangePID storeChangeSet(MCRAltoChangeSet changeSet) throws UnsupportedEncodingException {
        MCRJerseyUtil.checkPermission(changeSet.getDerivateID(), MCRALTOUtil.EDIT_ALTO_PERMISSION);
        MCRStoredChangeSet storedChangeSet = changeSetStore.storeChangeSet(changeSet);
        return new MCRAltoChangePID(storedChangeSet.getPid());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/update/{pid}")
    public MCRAltoChangePID updateChangeSet(MCRAltoChangeSet changeSet, @PathParam("pid") Integer pid)
        throws UnsupportedEncodingException {

        String currentSessionID = MCRSessionMgr.getCurrentSessionID();
        if (currentSessionID != null && !currentSessionID.equals(changeSetStore.get(pid).getSessionID())) {
            MCRJerseyUtil.checkPermission(changeSet.getDerivateID(), MCRALTOUtil.REVIEW_ALTO_PERMISSION);
        }

        MCRStoredChangeSet storedChangeSet = changeSetStore.updateChangeSet(pid, changeSet);

        if (storedChangeSet == null) {
            return storeChangeSet(changeSet);
        } else {
            return new MCRAltoChangePID(storedChangeSet.getPid());
        }
    }

    @POST
    @Path("/apply/{pid}")
    public String applyChangeSet(@PathParam("pid") Integer pid) {
        MCRStoredChangeSet storedChangeSet = changeSetStore.get(pid);
        MCRJerseyUtil.checkPermission(storedChangeSet.getDerivateID(), MCRALTOUtil.REVIEW_ALTO_PERMISSION);

        try {
            changeApplier.applyChange(storedChangeSet.getChangeSet());
        } catch (Throwable e) {
            throw e;
        }

        storedChangeSet.setApplied(new Date());

        return "";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes()
    @Path("/delete/{pid}")
    public int deleteChangeSet(@PathParam("pid") Integer pid) {
        String currentSessionID = MCRSessionMgr.getCurrentSessionID();
        MCRStoredChangeSet storedChangeSet = changeSetStore.get(pid);
        if (currentSessionID != null && !currentSessionID.equals(storedChangeSet.getSessionID())) {
            MCRJerseyUtil.checkPermission(storedChangeSet.getDerivateID(), MCRALTOUtil.REVIEW_ALTO_PERMISSION);
        }

        changeSetStore.delete(pid);
        return pid;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public List<MCRStoredChangeSet> listChangeSets(
        @DefaultValue("0") @QueryParam("start") long start,
        @DefaultValue("10") @QueryParam("count") long count,
        @QueryParam("derivate") String derivate,
        @QueryParam("session") String session) {

        String currentSessionID = MCRSessionMgr.getCurrentSessionID();
        if (currentSessionID == null || !currentSessionID.equals(session)) {
            MCRJerseyUtil.checkPermission(MCRALTOUtil.REVIEW_ALTO_PERMISSION);
        }

        List<MCRStoredChangeSet> list;
        if (derivate == null && session == null) {
            list = changeSetStore.list(start, count);
        } else if (derivate != null) {
            list = changeSetStore.listByDerivate(start, count, derivate);
        } else {
            list = changeSetStore.listBySessionID(start, count, session);
        }
        return list;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/list/count")
    public long count() {
        return changeSetStore.count();
    }
}
