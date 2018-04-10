package org.mycore.pi;

import java.util.concurrent.ExecutionException;

import org.mycore.common.MCRException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

public class MCRPIRegisterJobAction extends MCRJobAction {

    public MCRPIRegisterJobAction() {
    }

    public MCRPIRegisterJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        MCRPIJobService<MCRPersistentIdentifier> registrationService = getRegistrationService();
        return registrationService.getJobInformation(this.job.getParameters()).orElseGet(() -> {
            String action = getAction().toString();
            String registrationServiceID = getRegistrationServiceID();
            return registrationServiceID + " - " + action;
        });
    }

    private MCRPIJobService<MCRPersistentIdentifier> getRegistrationService() {
        String registrationServiceID = getRegistrationServiceID();
        return (MCRPIJobService<MCRPersistentIdentifier>) MCRPIServiceManager.getInstance()
            .getRegistrationService(registrationServiceID);
    }

    private MCRPIJobService.PiJobAction getAction() {
        return MCRPIJobService.PiJobAction.valueOf(this.job.getParameter("action"));
    }

    private String getRegistrationServiceID() {
        return this.job.getParameter("registrationServiceID");
    }

    @Override
    public void execute() throws ExecutionException {
        try {
            getRegistrationService().delegateAction(this.job.getParameters());
        } catch (MCRPersistentIdentifierException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            getRegistrationService().delegateRollback(this.job.getParameters());
        } catch (MCRPersistentIdentifierException e) {
            throw new MCRException(e);
        }
    }

}
