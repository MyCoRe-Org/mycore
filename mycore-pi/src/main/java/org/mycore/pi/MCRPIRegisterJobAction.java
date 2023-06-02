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

package org.mycore.pi;

import java.util.concurrent.ExecutionException;

import org.mycore.common.MCRException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

public class MCRPIRegisterJobAction extends MCRJobAction {

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
